package com.splitease.presentation.screens.groupDetails

import com.splitease.domain.model.Group

/**
 * UI state for the Group Details screen.
 * Follows unidirectional data flow — the ViewModel maps domain results to this sealed hierarchy.
 */
sealed interface GroupDetailsUiState {
    data object Loading : GroupDetailsUiState
    data class Success(
        val group: Group,
        /** Sum of all expense amounts for this group (excludes settlement payments). */
        val totalSpentMinorUnits: Long = 0L,
    ) : GroupDetailsUiState
    data object NotFound : GroupDetailsUiState
    data class Error(val message: String) : GroupDetailsUiState
}
