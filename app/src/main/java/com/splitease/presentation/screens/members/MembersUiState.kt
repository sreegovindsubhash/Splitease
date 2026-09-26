package com.splitease.presentation.screens.members

import com.splitease.domain.model.Member

/**
 * UI state for the Members screen.
 * Uses a single data class so edits/dialogs can be composed on top of the member list
 * without losing the list during transitions.
 */
data class MembersUiState(
    val isLoading: Boolean = true,
    val members: List<Member> = emptyList(),
    val groupId: Long = 0L,
    val groupNotFound: Boolean = false,
    val errorMessage: String? = null,

    // Add / Edit sheet state
    val isSheetOpen: Boolean = false,
    val editingMember: Member? = null,      // null = adding, non-null = editing
    val nameInput: String = "",
    val nameError: String? = null,
    val isSaving: Boolean = false,

    // Delete confirmation
    val memberPendingDelete: Member? = null, // non-null = confirmation dialog shown
) {
    /** True when we have finished loading and there are no members yet. */
    val isEmpty: Boolean get() = !isLoading && members.isEmpty() && !groupNotFound

    /** Convenience label for the sheet title. */
    val sheetTitle: String get() = if (editingMember == null) "Add member" else "Edit member"

    /** Convenience label for the confirm button. */
    val sheetActionLabel: String get() = if (editingMember == null) "Add" else "Save"
}
