package com.splitease.presentation.screens.groups

import com.splitease.domain.model.Group

/**
 * UI state for the Groups list screen.
 * Follows unidirectional data flow — the ViewModel maps domain models to this sealed hierarchy.
 */
sealed interface GroupsUiState {
    data object Loading : GroupsUiState
    data object Empty : GroupsUiState
    data class Success(val groups: List<Group>) : GroupsUiState
    data class Error(val message: String) : GroupsUiState
}
