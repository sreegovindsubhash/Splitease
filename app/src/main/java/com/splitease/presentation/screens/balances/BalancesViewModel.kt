package com.splitease.presentation.screens.balances

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splitease.domain.repository.ExpenseRepository
import com.splitease.domain.repository.GroupRepository
import com.splitease.domain.repository.MemberRepository
import com.splitease.domain.repository.SettlementPaymentRepository
import com.splitease.domain.usecase.CalculateMemberBalancesUseCase
import com.splitease.presentation.screens.summary.SummaryViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BalancesViewModel(
    private val groupId: Long,
    private val groupRepository: GroupRepository,
    private val memberRepository: MemberRepository,
    private val expenseRepository: ExpenseRepository,
    private val settlementPaymentRepository: SettlementPaymentRepository,
    private val calculateBalances: CalculateMemberBalancesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BalancesUiState(groupId = groupId))
    val uiState: StateFlow<BalancesUiState> = _uiState.asStateFlow()

    init {
        if (groupId <= 0L) {
            _uiState.update { it.copy(isLoading = false, groupNotFound = true) }
        } else {
            observeBalances()
        }
    }

    private fun observeBalances() {
        viewModelScope.launch {
            // Load static group metadata once; it rarely changes and we only need name + currency.
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
                    it.copy(isLoading = false, errorMessage = e.message ?: "Failed to load group.")
                }
                return@launch
            }

            // Combine expenses + splits + members + payments reactively.
            // Any change to any of these four flows triggers a balance recalculation.
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
                            errorMessage = e.message ?: "Failed to load balances.",
                        )
                    }
                }
                .collect { data ->
                    val memberNames = data.members.associate { it.id to it.name }
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

                    // Apply recorded settlement payments on top of raw expense balances so
                    // each member's CURRENT outstanding position is shown — identical logic to
                    // SummaryViewModel.computeAdjustedBalances, reused directly.
                    val adjustedBalances =
                        SummaryViewModel.computeAdjustedBalances(balances, data.payments)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            balances = balances,
                            adjustedBalances = adjustedBalances,
                            errorMessage = null,
                        )
                    }
                }
        }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
