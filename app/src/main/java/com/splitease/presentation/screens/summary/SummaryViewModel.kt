package com.splitease.presentation.screens.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splitease.domain.model.MemberBalance
import com.splitease.domain.model.SettlementPayment
import com.splitease.domain.repository.ExpenseRepository
import com.splitease.domain.repository.GroupRepository
import com.splitease.domain.repository.MemberRepository
import com.splitease.domain.repository.SettlementPaymentRepository
import com.splitease.domain.usecase.ApplySettlementPaymentsUseCase
import com.splitease.domain.usecase.CalculateMemberBalancesUseCase
import com.splitease.domain.usecase.GetSettlementsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Group Summary / Financial Overview screen.
 *
 * Aggregates data from the existing repositories and use-cases — no financial
 * logic is duplicated here.
 *
 * Data sources:
 *  - Total spent          : CalculateMemberBalancesUseCase (sum of totalPaid; unaffected by payments)
 *  - Adjusted balances    : raw balances + payment adjustments (see [computeAdjustedBalances])
 *  - Settlements          : GetSettlementsUseCase → ApplySettlementPaymentsUseCase (same as SettlementScreen)
 *  - Recent expenses      : ExpenseRepository.getExpensesForGroup (latest RECENT_EXPENSE_LIMIT, no JOIN)
 *
 * Reactive: any change to expenses, splits, members, or payments triggers a re-emission.
 */
class SummaryViewModel(
    private val groupId: Long,
    private val groupRepository: GroupRepository,
    private val memberRepository: MemberRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementPaymentRepository: SettlementPaymentRepository,
    private val calculateBalances: CalculateMemberBalancesUseCase,
    private val getSettlements: GetSettlementsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SummaryUiState(groupId = groupId))
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    init {
        if (groupId <= 0L) {
            _uiState.update { it.copy(isLoading = false, groupNotFound = true) }
        } else {
            observeSummary()
        }
    }

    private fun observeSummary() {
        viewModelScope.launch {
            // Load group metadata once (name + currency).
            // This data rarely changes; if it did change we'd need observeGroupById, but
            // the existing Settlement/Balances screens use the same one-shot pattern.
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

            // Combine all four reactive streams:
            //   expenses, splits, members, payments
            // Any change to any stream triggers a full recalculation.
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
                            errorMessage = e.message ?: "Failed to load summary data.",
                        )
                    }
                }
                .collect { data ->
                    val memberNames = data.members.associate { it.id to it.name }

                    // ── Balances ────────────────────────────────────────────────
                    val balances = try {
                        calculateBalances(
                            groupId = groupId,
                            expenses = data.expenses,
                            splits = data.splits,
                            memberNames = memberNames,
                        )
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = e.message ?: "Failed to calculate balances.",
                            )
                        }
                        return@collect
                    }

                    // ── Settlements ─────────────────────────────────────────────
                    // Use the same pipeline as SettlementViewModel so Summary and
                    // Settlement can never disagree.
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

                    val outstandingSettlements =
                        ApplySettlementPaymentsUseCase(suggested, data.payments)

                    // ── Adjusted balances ───────────────────────────────────────
                    // Apply recorded payments on top of raw expense balances so the
                    // Balances section shows each member's CURRENT position.
                    // Total spent is derived from raw balances and is NOT changed here.
                    val adjustedBalances = computeAdjustedBalances(balances, data.payments)

                    // ── Recent expenses ─────────────────────────────────────────
                    // Sort by date descending, then take the most recent few.
                    // No SQL JOIN — operates on the expense list from the Flow.
                    val recentExpenses = data.expenses
                        .sortedByDescending { it.date }
                        .take(RECENT_EXPENSE_LIMIT)
                        .map { expense ->
                            RecentExpenseItem(
                                id = expense.id,
                                description = expense.description,
                                amountMinorUnits = expense.amountMinorUnits,
                                payerName = memberNames[expense.paidByMemberId]
                                    ?: "Unknown member",
                                dateMillis = expense.date,
                            )
                        }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            balances = balances,
                            adjustedBalances = adjustedBalances,
                            outstandingSettlements = outstandingSettlements,
                            recentExpenses = recentExpenses,
                            hasExpenses = data.expenses.isNotEmpty(),
                            errorMessage = null,
                        )
                    }
                }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    companion object {
        /**
         * Adjusts raw expense-derived [MemberBalance] net positions by applying
         * recorded [SettlementPayment]s.
         *
         * For each payment (debtor → creditor, amount):
         *   debtor's  adjustedNet += amount  (they paid, so they owe less)
         *   creditor's adjustedNet -= amount  (they received, so they are owed less)
         *
         * The adjustment preserves the invariant that the sum of all adjusted nets
         * equals zero (paying a debt transfers net from creditor to debtor's ledger).
         *
         * All arithmetic is Long only — no Double/Float.
         *
         * @param rawBalances  Output of CalculateMemberBalancesUseCase.
         * @param payments     Recorded settlement payments for the group.
         * @return             One [AdjustedMemberBalance] per member, in the same order
         *                     as [rawBalances].
         */
        internal fun computeAdjustedBalances(
            rawBalances: List<MemberBalance>,
            payments: List<SettlementPayment>,
        ): List<AdjustedMemberBalance> {
            if (payments.isEmpty()) {
                // Fast path: no payments → adjusted net equals raw net.
                return rawBalances.map { mb ->
                    AdjustedMemberBalance(
                        memberId = mb.memberId,
                        memberName = mb.memberName,
                        adjustedNetMinorUnits = mb.netMinorUnits,
                    )
                }
            }

            // Start from raw net per member.
            val netByMember = mutableMapOf<Long, Long>()
            for (mb in rawBalances) {
                netByMember[mb.memberId] = mb.netMinorUnits
            }

            // Apply each payment: debtor gains (owes less), creditor loses (is owed less).
            for (payment in payments) {
                val debtorId = payment.debtorMemberId
                val creditorId = payment.creditorMemberId
                val amount = payment.amountMinorUnits

                netByMember[debtorId] =
                    Math.addExact(netByMember[debtorId] ?: 0L, amount)
                netByMember[creditorId] =
                    Math.addExact(netByMember[creditorId] ?: 0L, -amount)
            }

            // Rebuild in the same order as rawBalances so ordering is stable.
            return rawBalances.map { mb ->
                AdjustedMemberBalance(
                    memberId = mb.memberId,
                    memberName = mb.memberName,
                    adjustedNetMinorUnits = netByMember[mb.memberId] ?: mb.netMinorUnits,
                )
            }
        }
    }

    fun retry() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        observeSummary()
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
