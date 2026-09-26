package com.splitease.presentation.screens.expenses

import com.splitease.domain.model.ExpenseCategory
import com.splitease.domain.model.Member
import com.splitease.domain.model.SplitMethod

/**
 * Immutable UI state for the Add/Edit Expense screen.
 *
 * All monetary values are Long minor units. The [amountInput] field holds the raw
 * user-typed string so the UI can round-trip it without loss; it is only converted to
 * Long when the user attempts to save.
 */
data class AddEditExpenseUiState(
    // ── Context ───────────────────────────────────────────────────────────────
    val isLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val editingExpenseId: Long = 0L,
    val groupId: Long = 0L,
    val currencyCode: String = "INR",
    val groupMembers: List<Member> = emptyList(),

    // ── Core fields ───────────────────────────────────────────────────────────
    val descriptionInput: String = "",
    val descriptionError: String? = null,

    /** Raw user input string — may contain "100", "100.50", etc. */
    val amountInput: String = "",
    val amountError: String? = null,

    val selectedPayerId: Long = 0L,
    val payerError: String? = null,

    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val note: String = "",

    // ── Participants ──────────────────────────────────────────────────────────
    /** Set of member ids currently selected as participants. */
    val selectedParticipantIds: Set<Long> = emptySet(),
    val participantsError: String? = null,

    // ── Split method ──────────────────────────────────────────────────────────
    val splitMethod: SplitMethod = SplitMethod.EQUAL,

    /**
     * For EXACT split: maps memberId → raw input string typed by the user.
     * Displayed as-is; validated on save.
     */
    val exactAmountInputs: Map<Long, String> = emptyMap(),

    /**
     * For PERCENTAGE split: maps memberId → raw percentage string (e.g. "33.33").
     * Stored as display strings; converted to basis points on validation.
     */
    val percentageInputs: Map<Long, String> = emptyMap(),

    /**
     * For SHARES split: maps memberId → raw shares string (e.g. "1", "2").
     */
    val sharesInputs: Map<Long, String> = emptyMap(),

    // ── Derived / validation ──────────────────────────────────────────────────
    /**
     * Live split preview: memberId → computed share in Long minor units.
     * Empty when the split is invalid/cannot be computed.
     */
    val splitPreview: Map<Long, Long> = emptyMap(),

    /** Human-readable description of the current split validity. */
    val splitValidationMessage: String = "",

    /** True when all current inputs form a valid, saveable expense. */
    val isSplitValid: Boolean = false,

    val splitError: String? = null,

    // ── Submission ────────────────────────────────────────────────────────────
    val isSaving: Boolean = false,
    val savedExpenseId: Long? = null,   // set after successful save → triggers navigation
    val errorMessage: String? = null,
) {
    val selectedParticipants: List<Member>
        get() = groupMembers.filter { it.id in selectedParticipantIds }

    val hasMembers: Boolean get() = groupMembers.isNotEmpty()

    val formTitle: String get() = if (isEditMode) "Edit Expense" else "Add Expense"
}
