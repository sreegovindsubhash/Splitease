package com.splitease.presentation.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splitease.domain.model.Expense
import com.splitease.domain.repository.GroupRepository
import com.splitease.domain.repository.MemberRepository
import com.splitease.domain.usecase.DeleteExpenseUseCase
import com.splitease.domain.usecase.GetExpensesForGroupUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExpensesViewModel(
    private val groupId: Long,
    private val getExpensesForGroupUseCase: GetExpensesForGroupUseCase,
    private val deleteExpenseUseCase: DeleteExpenseUseCase,
    private val groupRepository: GroupRepository,
    private val memberRepository: MemberRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpensesUiState(groupId = groupId))
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    init {
        if (groupId <= 0L) {
            _uiState.update { it.copy(isLoading = false, groupNotFound = true) }
        } else {
            observeData()
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            // Load group metadata once
            try {
                val group = groupRepository.getGroupById(groupId)
                if (group == null) {
                    _uiState.update { it.copy(isLoading = false, groupNotFound = true) }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        currencyCode = group.currencyCode,
                        groupName = group.name,
                    )
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

            // Combine expenses + members reactively
            combine(
                getExpensesForGroupUseCase(groupId),
                memberRepository.getMembersForGroup(groupId),
            ) { expenses, members -> expenses to members }
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Failed to load expenses.",
                        )
                    }
                }
                .collect { (expenses, members) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            expenses = expenses,
                            members = members,
                            errorMessage = null,
                        )
                    }
                }
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun onRequestDelete(expense: Expense) {
        _uiState.update { it.copy(expensePendingDelete = expense) }
    }

    fun onDismissDeleteConfirmation() {
        _uiState.update { it.copy(expensePendingDelete = null) }
    }

    fun onConfirmDelete() {
        val expense = _uiState.value.expensePendingDelete ?: return
        _uiState.update { it.copy(expensePendingDelete = null) }

        viewModelScope.launch {
            try {
                deleteExpenseUseCase(expense)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.message ?: "Failed to delete expense.")
                }
            }
        }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
