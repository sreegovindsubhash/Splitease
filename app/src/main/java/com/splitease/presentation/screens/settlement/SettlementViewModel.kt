package com.splitease.presentation.screens.settlement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splitease.domain.model.SettlementPayment
import com.splitease.domain.model.SettlementTransaction
import com.splitease.domain.repository.ExpenseRepository
import com.splitease.domain.repository.GroupRepository
import com.splitease.domain.repository.MemberRepository
import com.splitease.domain.repository.SettlementPaymentRepository
import com.splitease.domain.usecase.ApplySettlementPaymentsUseCase
import com.splitease.domain.usecase.GetSettlementsUseCase
import com.splitease.util.MoneyFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettlementViewModel(
    private val groupId: Long,
    private val groupRepository: GroupRepository,
    private val memberRepository: MemberRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementPaymentRepository: SettlementPaymentRepository,
    private val getSettlements: GetSettlementsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettlementUiState(groupId = groupId))
    val uiState: StateFlow<SettlementUiState> = _uiState.asStateFlow()

    init {
        if (groupId <= 0L) {
            _uiState.update { it.copy(isLoading = false, groupNotFound = true) }
        } else {
            observeSettlements()
        }
    }

    private fun observeSettlements() {
        viewModelScope.launch {
            // Load static group metadata (name, currency) once.
            try {
                val group = groupRepository.getGroupById(groupId)
                if (group == null) {
                    _uiState.update { it.copy(isLoading = false, groupNotFound = true) }
                    return@launch
                }
                _uiState.update {
                    it.copy(groupName = group.name, currencyCode = group.currencyCode)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load group.",
                    )
                }
                return@launch
            }

            // Reactively combine expenses + splits + members + recorded payments.
            // Any change to any of the four flows triggers a full recalculation.
            combine(
                expenseRepository.getExpensesForGroup(groupId),
                expenseRepository.getSplitsForGroup(groupId),
                memberRepository.getMembersForGroup(groupId),
                settlementPaymentRepository.getPaymentsForGroup(groupId),
            ) { expenses, splits, members, payments ->
                object {
                    val expenses = expenses
                    val splits = splits
                    val members = members
                    val payments = payments
                }
            }
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Failed to load settlement data.",
                        )
                    }
                }
                .collect { data ->
                    val memberNames = data.members.associate { it.id to it.name }
                    val suggested = try {
                        getSettlements(
                            groupId = groupId,
                            expenses = data.expenses,
                            splits = data.splits,
                            memberNames = memberNames,
                        )
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = e.message ?: "Failed to calculate settlements.",
                            )
                        }
                        return@collect
                    }

                    val remaining = ApplySettlementPaymentsUseCase(suggested, data.payments)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            settlements = remaining,
                            recentPayments = data.payments,
                            memberNames = memberNames,
                            hasExpenses = data.expenses.isNotEmpty(),
                            errorMessage = null,
                        )
                    }
                }
        }
    }

    // ── Dialog ────────────────────────────────────────────────────────────────

    /** Open the confirmation dialog, defaulting the amount to the full outstanding balance. */
    fun onMarkAsPaidClick(settlement: SettlementTransaction) {
        _uiState.update {
            it.copy(
                pendingPaymentDialog = PendingPaymentDialog(
                    settlement = settlement,
                    amountMinorUnits = settlement.amountMinorUnits,
                ),
            )
        }
    }

    /** Update the payment amount in the dialog (user is typing). */
    fun onPaymentAmountChanged(amountMinorUnits: Long) {
        _uiState.update { state ->
            val dialog = state.pendingPaymentDialog ?: return@update state
            val error = when {
                amountMinorUnits <= 0L -> "Amount must be greater than zero"
                amountMinorUnits > dialog.settlement.amountMinorUnits ->
                    "Amount cannot exceed the outstanding balance"
                else -> null
            }
            state.copy(
                pendingPaymentDialog = dialog.copy(
                    amountMinorUnits = amountMinorUnits,
                    amountError = error,
                ),
            )
        }
    }

    fun onPaymentDialogDismissed() {
        _uiState.update { it.copy(pendingPaymentDialog = null) }
    }

    /** Validate and record the payment from the active dialog. */
    fun onConfirmPayment() {
        val dialog = _uiState.value.pendingPaymentDialog ?: return
        val settlement = dialog.settlement
        val amount = dialog.amountMinorUnits

        // Final validation
        if (amount <= 0L) {
            _uiState.update {
                it.copy(
                    pendingPaymentDialog = dialog.copy(
                        amountError = "Amount must be greater than zero",
                    ),
                )
            }
            return
        }
        if (amount > settlement.amountMinorUnits) {
            _uiState.update {
                it.copy(
                    pendingPaymentDialog = dialog.copy(
                        amountError = "Amount cannot exceed the outstanding balance",
                    ),
                )
            }
            return
        }

        _uiState.update { it.copy(pendingPaymentDialog = null) }

        viewModelScope.launch {
            try {
                val payment = SettlementPayment(
                    groupId = groupId,
                    debtorMemberId = settlement.fromMemberId,
                    creditorMemberId = settlement.toMemberId,
                    amountMinorUnits = amount,
                )
                val savedId = settlementPaymentRepository.recordPayment(payment)
                val savedPayment = payment.copy(id = savedId)
                val amountFormatted = MoneyFormatter.format(amount, _uiState.value.currencyCode)
                _uiState.update {
                    it.copy(
                        lastRecordedPayment = savedPayment,
                        snackbarMessage = "Payment recorded: ${settlement.fromMemberName} paid ${settlement.toMemberName} $amountFormatted",
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to record payment.")
                }
            }
        }
    }

    // ── Undo / Reverse ────────────────────────────────────────────────────────

    fun onUndoLastPayment() {
        val payment = _uiState.value.lastRecordedPayment ?: return
        viewModelScope.launch {
            try {
                settlementPaymentRepository.deletePayment(payment.id)
                _uiState.update {
                    it.copy(
                        lastRecordedPayment = null,
                        snackbarMessage = "Payment reversed",
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to reverse payment.")
                }
            }
        }
    }

    fun onReversePayment(payment: SettlementPayment) {
        viewModelScope.launch {
            try {
                settlementPaymentRepository.deletePayment(payment.id)
                _uiState.update { it.copy(snackbarMessage = "Payment removed") }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to remove payment.")
                }
            }
        }
    }

    // ── Snackbar / Error ──────────────────────────────────────────────────────

    fun onSnackbarMessageConsumed() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
