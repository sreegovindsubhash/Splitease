package com.splitease.presentation.screens.createGroup

/**
 * UI state for the Create Group screen.
 * Follows unidirectional data flow — the ViewModel maps domain results to this state.
 */
data class CreateGroupUiState(
    val name: String = "",
    val nameError: String? = null,
    val description: String = "",
    val currencyCode: String = "INR",
    val isSaving: Boolean = false,
    val savedGroupId: Long? = null,
    val errorMessage: String? = null,
)
