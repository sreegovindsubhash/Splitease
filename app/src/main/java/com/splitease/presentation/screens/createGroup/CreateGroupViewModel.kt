package com.splitease.presentation.screens.createGroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splitease.domain.usecase.CreateGroupUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateGroupViewModel(
    private val createGroupUseCase: CreateGroupUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = null, errorMessage = null) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun onCurrencyChange(value: String) {
        _uiState.update { it.copy(currencyCode = value) }
    }

    fun onCreateGroup() {
        val state = _uiState.value

        // Prevent duplicate submissions
        if (state.isSaving) return

        val trimmedName = state.name.trim()
        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(nameError = "Group name is required") }
            return
        }

        _uiState.update { it.copy(isSaving = true, nameError = null, errorMessage = null) }

        viewModelScope.launch {
            try {
                val groupId = createGroupUseCase(
                    name = trimmedName,
                    currencyCode = state.currencyCode,
                    description = state.description.trim(),
                )
                _uiState.update { it.copy(isSaving = false, savedGroupId = groupId) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Failed to create group. Please try again.",
                    )
                }
            }
        }
    }

    /** Call after the navigation side-effect has been consumed. */
    fun onNavigationConsumed() {
        _uiState.update { it.copy(savedGroupId = null) }
    }
}
