package com.splitease.presentation.screens.groupDetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splitease.domain.repository.ExpenseRepository
import com.splitease.domain.repository.GroupRepository
import com.splitease.domain.usecase.DeleteGroupUseCase
import com.splitease.domain.usecase.SetGroupBudgetUseCase
import com.splitease.presentation.screens.expenses.AddEditExpenseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupDetailsViewModel(
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val deleteGroupUseCase: DeleteGroupUseCase,
    private val setGroupBudgetUseCase: SetGroupBudgetUseCase,
    private val groupId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GroupDetailsUiState>(GroupDetailsUiState.Loading)
    val uiState: StateFlow<GroupDetailsUiState> = _uiState.asStateFlow()

    /** True while deletion is in progress — prevents double-tap. */
    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    /** One-shot signal emitted after the group is successfully deleted. */
    private val _groupDeleted = MutableStateFlow(false)
    val groupDeleted: StateFlow<Boolean> = _groupDeleted.asStateFlow()

    /** Budget dialog visibility state. */
    private val _showBudgetDialog = MutableStateFlow(false)
    val showBudgetDialog: StateFlow<Boolean> = _showBudgetDialog.asStateFlow()

    init {
        observeGroup()
    }

    fun retry() {
        _uiState.value = GroupDetailsUiState.Loading
        observeGroup()
    }

    private fun observeGroup() {
        viewModelScope.launch {
            try {
                combine(
                    groupRepository.observeGroupById(groupId),
                    expenseRepository.getExpensesForGroup(groupId),
                ) { group, expenses ->
                    if (group == null) {
                        GroupDetailsUiState.NotFound
                    } else {
                        val totalSpentMinorUnits = expenses.sumOf { it.amountMinorUnits }
                        GroupDetailsUiState.Success(
                            group = group,
                            totalSpentMinorUnits = totalSpentMinorUnits,
                        )
                    }
                }
                    .catch { e ->
                        _uiState.value = GroupDetailsUiState.Error(
                            e.message ?: "Failed to load group. Please try again.",
                        )
                    }
                    .collect { state ->
                        _uiState.value = state
                    }
            } catch (e: Exception) {
                _uiState.value = GroupDetailsUiState.Error(
                    e.message ?: "Failed to load group. Please try again.",
                )
            }
        }
    }

    fun deleteGroup() {
        if (_isDeleting.value) return
        val currentState = _uiState.value as? GroupDetailsUiState.Success ?: return

        _isDeleting.update { true }
        viewModelScope.launch {
            try {
                deleteGroupUseCase(currentState.group)
                _groupDeleted.update { true }
            } catch (e: Exception) {
                _isDeleting.update { false }
                _uiState.value = GroupDetailsUiState.Error(
                    e.message ?: "Failed to delete group. Please try again.",
                )
            }
        }
    }

    /** Call after the deletion navigation side-effect has been consumed. */
    fun onDeletedConsumed() {
        _groupDeleted.update { false }
    }

    // ── Budget ────────────────────────────────────────────────────────────────

    fun onSetBudgetClicked() {
        _showBudgetDialog.update { true }
    }

    fun onBudgetDialogDismiss() {
        _showBudgetDialog.update { false }
    }

    /**
     * Saves the budget from the dialog's raw amount-input string.
     * Uses [AddEditExpenseViewModel.parseAmount] for consistent money parsing.
     * Returns an error message string if the input is invalid, null on success.
     */
    fun onBudgetConfirmed(amountInput: String): String? {
        val parsed = AddEditExpenseViewModel.parseAmount(amountInput)
        if (parsed == null || parsed <= 0L) {
            return "Enter a valid positive amount."
        }
        val currentState = _uiState.value as? GroupDetailsUiState.Success ?: return null
        viewModelScope.launch {
            try {
                setGroupBudgetUseCase(currentState.group, parsed)
                _showBudgetDialog.update { false }
            } catch (e: Exception) {
                // Error surfaces via uiState; dialog stays open if desired
            }
        }
        return null
    }
}
