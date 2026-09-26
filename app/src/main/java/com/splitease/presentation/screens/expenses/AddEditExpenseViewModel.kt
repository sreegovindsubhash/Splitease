package com.splitease.presentation.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splitease.domain.model.Expense
import com.splitease.domain.model.ExpenseCategory
import com.splitease.domain.model.ExpenseSplit
import com.splitease.domain.model.SplitMethod
import com.splitease.domain.repository.GroupRepository
import com.splitease.domain.repository.MemberRepository
import com.splitease.domain.usecase.GetExpenseByIdUseCase
import com.splitease.domain.usecase.GetSplitsForExpenseUseCase
import com.splitease.domain.usecase.MoneySplitEngine
import com.splitease.domain.usecase.SaveExpenseUseCase
import com.splitease.domain.usecase.UpdateExpenseUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddEditExpenseViewModel(
    private val groupId: Long,
    private val editExpenseId: Long,           // 0 = adding new; > 0 = editing
    private val groupRepository: GroupRepository,
    private val memberRepository: MemberRepository,
    private val saveExpenseUseCase: SaveExpenseUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val getExpenseByIdUseCase: GetExpenseByIdUseCase,
    private val getSplitsForExpenseUseCase: GetSplitsForExpenseUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AddEditExpenseUiState(
            groupId = groupId,
            isEditMode = editExpenseId > 0L,
            editingExpenseId = editExpenseId,
        ),
    )
    val uiState: StateFlow<AddEditExpenseUiState> = _uiState.asStateFlow()

    init {
        loadContext()
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    private fun loadContext() {
        viewModelScope.launch {
            try {
                val group = groupRepository.getGroupById(groupId)
                    ?: run {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "Group not found.") }
                        return@launch
                    }

                val members = memberRepository.getMembersForGroup(groupId).first()

                _uiState.update {
                    it.copy(
                        currencyCode = group.currencyCode,
                        groupMembers = members,
                    )
                }

                if (editExpenseId > 0L) {
                    loadExistingExpense(members.map { it.id }.toSet())
                } else {
                    // Default: select all members as participants
                    val allIds = members.map { it.id }.toSet()
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            selectedParticipantIds = allIds,
                        ).recomputeSplit()
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load form data.",
                    )
                }
            }
        }
    }

    private suspend fun loadExistingExpense(memberIds: Set<Long>) {
        val expense = getExpenseByIdUseCase(editExpenseId)
            ?: run {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Expense not found.") }
                return
            }
        val splits = getSplitsForExpenseUseCase(editExpenseId).first()

        val participantIds = splits.map { it.memberId }.toSet()
        val exactInputs = splits.associate { it.memberId to minorToDisplayString(it.shareMinorUnits, expense.currencyCode) }
        val pctInputs: Map<Long, String> = emptyMap()
        val sharesInputs: Map<Long, String> = participantIds.associateWith { "1" }

        _uiState.update { state ->
            state.copy(
                isLoading = false,
                descriptionInput = expense.description,
                amountInput = minorToDisplayString(expense.amountMinorUnits, expense.currencyCode),
                selectedPayerId = expense.paidByMemberId,
                category = expense.category,
                note = expense.note,
                splitMethod = expense.splitMethod,
                selectedParticipantIds = participantIds,
                exactAmountInputs = exactInputs,
                percentageInputs = pctInputs,
                sharesInputs = sharesInputs,
            ).recomputeSplit()
        }
    }

    // ── Field handlers ────────────────────────────────────────────────────────

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(descriptionInput = value, descriptionError = null) }
    }

    fun onAmountChange(value: String) {
        _uiState.update { state ->
            state.copy(amountInput = value, amountError = null).recomputeSplit()
        }
    }

    fun onPayerSelected(memberId: Long) {
        _uiState.update { it.copy(selectedPayerId = memberId, payerError = null) }
    }

    fun onCategorySelected(category: ExpenseCategory) {
        _uiState.update { it.copy(category = category) }
    }

    fun onNoteChange(value: String) {
        _uiState.update { it.copy(note = value) }
    }

    fun onSplitMethodSelected(method: SplitMethod) {
        _uiState.update { state ->
            state.copy(splitMethod = method, splitError = null).recomputeSplit()
        }
    }

    fun onParticipantToggled(memberId: Long) {
        _uiState.update { state ->
            val updated = if (memberId in state.selectedParticipantIds) {
                state.selectedParticipantIds - memberId
            } else {
                state.selectedParticipantIds + memberId
            }
            state.copy(selectedParticipantIds = updated, participantsError = null).recomputeSplit()
        }
    }

    fun onExactAmountChanged(memberId: Long, value: String) {
        _uiState.update { state ->
            state.copy(
                exactAmountInputs = state.exactAmountInputs + (memberId to value),
                splitError = null,
            ).recomputeSplit()
        }
    }

    fun onPercentageChanged(memberId: Long, value: String) {
        _uiState.update { state ->
            state.copy(
                percentageInputs = state.percentageInputs + (memberId to value),
                splitError = null,
            ).recomputeSplit()
        }
    }

    fun onSharesChanged(memberId: Long, value: String) {
        _uiState.update { state ->
            state.copy(
                sharesInputs = state.sharesInputs + (memberId to value),
                splitError = null,
            ).recomputeSplit()
        }
    }

    // ── Split recomputation ───────────────────────────────────────────────────

    /** Pure extension that returns a new state with updated split preview/validation. */
    private fun AddEditExpenseUiState.recomputeSplit(): AddEditExpenseUiState {
        val amount = parseAmount(amountInput) ?: return copy(
            splitPreview = emptyMap(),
            isSplitValid = false,
            splitValidationMessage = if (amountInput.isBlank()) "" else "Enter a valid amount first.",
        )

        val participants = selectedParticipants
        if (participants.isEmpty()) {
            return copy(
                splitPreview = emptyMap(),
                isSplitValid = false,
                splitValidationMessage = "Select at least one participant.",
            )
        }

        return when (splitMethod) {
            SplitMethod.EQUAL -> computeEqual(amount, participants)
            SplitMethod.EXACT -> computeExact(amount, participants)
            SplitMethod.PERCENTAGE -> computePercentage(amount, participants)
            SplitMethod.SHARES -> computeShares(amount, participants)
        }
    }

    private fun AddEditExpenseUiState.computeEqual(
        amount: Long,
        participants: List<com.splitease.domain.model.Member>,
    ): AddEditExpenseUiState {
        return try {
            val allocations = MoneySplitEngine.split(
                totalMinor = amount,
                type = MoneySplitEngine.SplitType.EQUAL,
                participants = participants.map { MoneySplitEngine.Participant(it.id.toString()) },
            )
            val preview = allocations.associate { it.memberId.toLong() to it.amountMinor }
            copy(
                splitPreview = preview,
                isSplitValid = true,
                splitValidationMessage = "Split: ${MoneyUtils.format(amount, currencyCode)} / ${MoneyUtils.format(amount, currencyCode)} ✓",
                splitError = null,
            )
        } catch (e: IllegalArgumentException) {
            copy(splitPreview = emptyMap(), isSplitValid = false, splitValidationMessage = e.message ?: "Invalid split.", splitError = e.message)
        }
    }

    private fun AddEditExpenseUiState.computeExact(
        amount: Long,
        participants: List<com.splitease.domain.model.Member>,
    ): AddEditExpenseUiState {
        val amounts = participants.associate { m ->
            m.id to (parseAmount(exactAmountInputs[m.id] ?: "") ?: 0L)
        }
        val total = amounts.values.sum()
        val remaining = amount - total
        val isValid = remaining == 0L && amounts.values.all { it >= 0 }
        val message = if (isValid) {
            "Split: ${MoneyUtils.format(amount, currencyCode)} / ${MoneyUtils.format(amount, currencyCode)} ✓"
        } else {
            val sign = if (remaining > 0) "Remaining" else "Over by"
            "Split: ${MoneyUtils.format(total, currencyCode)} / ${MoneyUtils.format(amount, currencyCode)}  $sign: ${MoneyUtils.format(kotlin.math.abs(remaining), currencyCode)}"
        }
        return copy(
            splitPreview = amounts,
            isSplitValid = isValid,
            splitValidationMessage = message,
            splitError = if (isValid) null else message,
        )
    }

    private fun AddEditExpenseUiState.computePercentage(
        amount: Long,
        participants: List<com.splitease.domain.model.Member>,
    ): AddEditExpenseUiState {
        // Convert percentage strings to basis points (10000 = 100%)
        val basisPointsMap = participants.associate { m ->
            val pctStr = percentageInputs[m.id] ?: ""
            val bps = parseBasisPoints(pctStr)
            m.id to (bps ?: 0L)
        }
        val totalBps = basisPointsMap.values.sum()
        if (totalBps != BASIS_POINTS) {
            val pctStr = formatBasisPoints(totalBps)
            return copy(
                splitPreview = emptyMap(),
                isSplitValid = false,
                splitValidationMessage = "Percentages total $pctStr% — must equal 100%.",
                splitError = "Percentages must total 100%.",
            )
        }
        return try {
            val allocations = MoneySplitEngine.split(
                totalMinor = amount,
                type = MoneySplitEngine.SplitType.PERCENTAGE,
                participants = participants.map { MoneySplitEngine.Participant(it.id.toString()) },
                percentageBasisPoints = basisPointsMap.mapKeys { it.key.toString() },
            )
            val preview = allocations.associate { it.memberId.toLong() to it.amountMinor }
            copy(
                splitPreview = preview,
                isSplitValid = true,
                splitValidationMessage = "Split: ${MoneyUtils.format(amount, currencyCode)} / ${MoneyUtils.format(amount, currencyCode)} ✓",
                splitError = null,
            )
        } catch (e: IllegalArgumentException) {
            copy(splitPreview = emptyMap(), isSplitValid = false, splitValidationMessage = e.message ?: "Invalid split.", splitError = e.message)
        }
    }

    private fun AddEditExpenseUiState.computeShares(
        amount: Long,
        participants: List<com.splitease.domain.model.Member>,
    ): AddEditExpenseUiState {
        val sharesMap = participants.associate { m ->
            m.id to (sharesInputs[m.id]?.trim()?.toLongOrNull() ?: 0L)
        }
        if (sharesMap.values.any { it <= 0L }) {
            return copy(
                splitPreview = emptyMap(),
                isSplitValid = false,
                splitValidationMessage = "Each participant must have at least 1 share.",
                splitError = "Each participant needs a positive share count.",
            )
        }
        return try {
            val allocations = MoneySplitEngine.split(
                totalMinor = amount,
                type = MoneySplitEngine.SplitType.SHARES,
                participants = participants.map { MoneySplitEngine.Participant(it.id.toString()) },
                shares = sharesMap.mapKeys { it.key.toString() },
            )
            val preview = allocations.associate { it.memberId.toLong() to it.amountMinor }
            copy(
                splitPreview = preview,
                isSplitValid = true,
                splitValidationMessage = "Split: ${MoneyUtils.format(amount, currencyCode)} / ${MoneyUtils.format(amount, currencyCode)} ✓",
                splitError = null,
            )
        } catch (e: IllegalArgumentException) {
            copy(splitPreview = emptyMap(), isSplitValid = false, splitValidationMessage = e.message ?: "Invalid split.", splitError = e.message)
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun onSave() {
        val state = _uiState.value
        if (state.isSaving) return

        // Run all field validations
        val trimmedDesc = state.descriptionInput.trim()
        var hasErrors = false

        if (trimmedDesc.isBlank()) {
            _uiState.update { it.copy(descriptionError = "Description is required.") }
            hasErrors = true
        }

        val amount = parseAmount(state.amountInput)
        if (amount == null || amount <= 0L) {
            _uiState.update { it.copy(amountError = "Enter a valid positive amount.") }
            hasErrors = true
        }

        if (state.selectedPayerId <= 0L) {
            _uiState.update { it.copy(payerError = "Select who paid.") }
            hasErrors = true
        }

        if (state.selectedParticipantIds.isEmpty()) {
            _uiState.update { it.copy(participantsError = "Select at least one participant.") }
            hasErrors = true
        }

        if (!state.isSplitValid) {
            _uiState.update { it.copy(splitError = state.splitValidationMessage.ifBlank { "Fix the split before saving." }) }
            hasErrors = true
        }

        if (hasErrors) return

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val amountMinor = parseAmount(currentState.amountInput)!!
                val splits = buildSplits(currentState, amountMinor)
                val now = System.currentTimeMillis()

                if (currentState.isEditMode) {
                    val expense = Expense(
                        id = currentState.editingExpenseId,
                        groupId = groupId,
                        description = trimmedDesc,
                        amountMinorUnits = amountMinor,
                        currencyCode = currentState.currencyCode,
                        paidByMemberId = currentState.selectedPayerId,
                        category = currentState.category,
                        date = now,
                        note = currentState.note.trim(),
                        splitMethod = currentState.splitMethod,
                        updatedAt = now,
                    )
                    updateExpenseUseCase(expense, splits)
                    _uiState.update { it.copy(isSaving = false, savedExpenseId = expense.id) }
                } else {
                    val expense = Expense(
                        groupId = groupId,
                        description = trimmedDesc,
                        amountMinorUnits = amountMinor,
                        currencyCode = currentState.currencyCode,
                        paidByMemberId = currentState.selectedPayerId,
                        category = currentState.category,
                        date = now,
                        note = currentState.note.trim(),
                        splitMethod = currentState.splitMethod,
                        createdAt = now,
                        updatedAt = now,
                    )
                    val newId = saveExpenseUseCase(expense, splits)
                    _uiState.update { it.copy(isSaving = false, savedExpenseId = newId) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Failed to save expense.",
                    )
                }
            }
        }
    }

    private fun buildSplits(state: AddEditExpenseUiState, amountMinor: Long): List<ExpenseSplit> =
        state.splitPreview.map { (memberId, share) ->
            ExpenseSplit(
                expenseId = if (state.isEditMode) state.editingExpenseId else 0L,
                memberId = memberId,
                shareMinorUnits = share,
            )
        }

    fun onNavigationConsumed() {
        _uiState.update { it.copy(savedExpenseId = null) }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    companion object {
        private const val BASIS_POINTS = 10_000L

        /**
         * Parses a display string like "100", "100.50", "100,50" into Long minor units.
         * Returns null if the input is blank or not parseable.
         * NEVER goes through Double: uses only integer arithmetic.
         */
        fun parseAmount(input: String): Long? {
            val s = input.trim().replace(",", ".")
            if (s.isBlank()) return null
            if (s.startsWith("-")) return null   // negative amounts rejected
            val dotIndex = s.indexOf('.')
            return if (dotIndex == -1) {
                val major = s.toLongOrNull() ?: return null
                major * 100L
            } else {
                val majorStr = s.substring(0, dotIndex)
                val minorStr = s.substring(dotIndex + 1).take(2).padEnd(2, '0')
                val major = if (majorStr.isEmpty()) 0L else majorStr.toLongOrNull() ?: return null
                val minor = minorStr.toLongOrNull() ?: return null
                major * 100L + minor
            }
        }

        /**
         * Converts basis points string like "33.33" → 3333L.
         * "100" → 10000L.
         */
        fun parseBasisPoints(input: String): Long? {
            val s = input.trim().replace(",", ".")
            if (s.isBlank()) return null
            val dotIndex = s.indexOf('.')
            return if (dotIndex == -1) {
                val whole = s.toLongOrNull() ?: return null
                whole * 100L
            } else {
                val wholeStr = s.substring(0, dotIndex)
                val fracStr = s.substring(dotIndex + 1).take(2).padEnd(2, '0')
                val whole = if (wholeStr.isEmpty()) 0L else wholeStr.toLongOrNull() ?: return null
                val frac = fracStr.toLongOrNull() ?: return null
                whole * 100L + frac
            }
        }

        /** Formats basis points as "33.33" or "100.00" for display. */
        fun formatBasisPoints(bps: Long): String {
            val whole = bps / 100L
            val frac = bps % 100L
            return "$whole.${frac.toString().padStart(2, '0')}"
        }

        /** Converts Long minor units back to a display string like "100.50". */
        fun minorToDisplayString(minor: Long, currencyCode: String): String {
            // Use 2 decimal places for all currencies in input fields
            val major = minor / 100L
            val frac = minor % 100L
            return "$major.${frac.toString().padStart(2, '0')}"
        }
    }
}

/** Thin shim to access MoneyFormatter without importing util directly from domain layer. */
private object MoneyUtils {
    fun format(minor: Long, currencyCode: String): String =
        com.splitease.util.MoneyFormatter.format(minor, currencyCode)
}
