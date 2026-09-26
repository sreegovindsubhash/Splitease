package com.splitease.presentation.screens.editGroup

/**
 * UI state for the Edit Group screen.
 * Currency is read-only — only name and description may be changed.
 */
data class EditGroupUiState(
    val groupId: Long = 0L,
    val name: String = "",
    val nameError: String? = null,
    val description: String = "",
    val currencyCode: String = "",   // displayed but not editable
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,    // one-shot signal to navigate back
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)
