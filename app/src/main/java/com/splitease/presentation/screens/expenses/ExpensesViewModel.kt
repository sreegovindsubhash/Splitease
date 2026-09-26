package com.splitease.presentation.screens.expenses

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splitease.domain.model.Expense
import com.splitease.domain.repository.GroupRepository
import com.splitease.domain.repository.MemberRepository
import com.splitease.domain.usecase.DeleteExpenseUseCase
import com.splitease.domain.usecase.GetExpensesForGroupUseCase
import com.splitease.util.ExpenseCsvExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // ── CSV Export ────────────────────────────────────────────────────────────

    /**
     * Called when the user taps "Export CSV".
     * Sets [ExpensesUiState.pendingCsvFilename] to trigger the document-creation launcher
     * in the Composable. The suggested filename uses the group name.
     */
    fun onExportCsvClicked() {
        val groupName = _uiState.value.groupName
            .ifBlank { "Expenses" }
            .replace(Regex("[^A-Za-z0-9_\\-]"), "_")
        val filename = "SplitEase_${groupName}_Expenses.csv"
        _uiState.update { it.copy(pendingCsvFilename = filename) }
    }

    /**
     * Called by the Composable once it has consumed [ExpensesUiState.pendingCsvFilename]
     * and launched the document-creation activity. Clears the trigger so it is not
     * fired again on recomposition.
     */
    fun onCsvFilenameLauncherConsumed() {
        _uiState.update { it.copy(pendingCsvFilename = null) }
    }

    /**
     * Called when Android's document-creation flow returns a [Uri] selected by the user.
     * Generates the CSV and writes it to the URI on the IO dispatcher.
     *
     * @param uri     The URI returned by [ActivityResultContracts.CreateDocument].
     * @param context Application context used to open the output stream.
     */
    fun onCsvUriReady(uri: Uri, context: Context) {
        val state = _uiState.value
        val memberNames = state.members.associate { it.id to it.name }
        val expenses = state.expenses
        val currencyCode = state.currencyCode

        viewModelScope.launch {
            try {
                val csv = ExpenseCsvExporter.generate(expenses, memberNames, currencyCode)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(csv.toByteArray(Charsets.UTF_8))
                    } ?: error("Could not open output stream for the selected file.")
                }
                _uiState.update { it.copy(csvExportMessage = "CSV exported successfully") }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(csvExportMessage = "Export failed: ${e.message ?: "Unknown error"}")
                }
            }
        }
    }

    /** Clears the one-shot CSV export snackbar message once it has been shown. */
    fun onCsvMessageConsumed() {
        _uiState.update { it.copy(csvExportMessage = null) }
    }
}
