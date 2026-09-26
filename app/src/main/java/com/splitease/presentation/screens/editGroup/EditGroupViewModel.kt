package com.splitease.presentation.screens.editGroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splitease.domain.repository.GroupRepository
import com.splitease.domain.usecase.UpdateGroupUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditGroupViewModel(
    private val groupRepository: GroupRepository,
    private val updateGroupUseCase: UpdateGroupUseCase,
    private val groupId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditGroupUiState())
    val uiState: StateFlow<EditGroupUiState> = _uiState.asStateFlow()

    init {
        loadGroup()
    }

    private fun loadGroup() {
        viewModelScope.launch {
            val group = groupRepository.getGroupById(groupId)
            if (group != null) {
                _uiState.update {
                    it.copy(
                        groupId = group.id,
                        name = group.name,
                        description = group.description,
                        currencyCode = group.currencyCode,
                        isLoading = false,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Group not found.",
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = null, errorMessage = null) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun onSave() {
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
                val existing = groupRepository.getGroupById(groupId)
                    ?: throw IllegalStateException("Group not found.")
                updateGroupUseCase(
                    existing = existing,
                    newName = trimmedName,
                    newDescription = state.description,
                )
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Failed to save. Please try again.",
                    )
                }
            }
        }
    }

    /** Call after the navigation side-effect has been consumed. */
    fun onSavedConsumed() {
        _uiState.update { it.copy(isSaved = false) }
    }
}
