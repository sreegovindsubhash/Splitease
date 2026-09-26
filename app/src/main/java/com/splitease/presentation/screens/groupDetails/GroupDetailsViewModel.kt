package com.splitease.presentation.screens.groupDetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splitease.domain.repository.GroupRepository
import com.splitease.domain.usecase.DeleteGroupUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupDetailsViewModel(
    private val groupRepository: GroupRepository,
    private val deleteGroupUseCase: DeleteGroupUseCase,
    private val groupId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GroupDetailsUiState>(GroupDetailsUiState.Loading)
    val uiState: StateFlow<GroupDetailsUiState> = _uiState.asStateFlow()

    /** True while deletion is in progress — prevents double-tap. */
    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    /** One-shot signal emitted after the group is successfully deleted. */
    private val _groupDeleted = MutableStateFlow(false)
    val groupDeleted: StateFlow<Boolean> = _groupDeleted.asStateFlow()

    init {
        observeGroup()
    }

    fun retry() {
        _uiState.value = GroupDetailsUiState.Loading
        observeGroup()
    }

    private fun observeGroup() {
        viewModelScope.launch {
            try {
                groupRepository.observeGroupById(groupId)
                    .catch { e ->
                        _uiState.value = GroupDetailsUiState.Error(
                            e.message ?: "Failed to load group. Please try again.",
                        )
                    }
                    .collect { group ->
                        _uiState.value = if (group != null) {
                            GroupDetailsUiState.Success(group)
                        } else {
                            GroupDetailsUiState.NotFound
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = GroupDetailsUiState.Error(
                    e.message ?: "Failed to load group. Please try again.",
                )
            }
        }
    }

    fun deleteGroup() {
        if (_isDeleting.value) return
        val currentState = _uiState.value as? GroupDetailsUiState.Success ?: return

        _isDeleting.update { true }
        viewModelScope.launch {
            try {
                deleteGroupUseCase(currentState.group)
                _groupDeleted.update { true }
            } catch (e: Exception) {
                _isDeleting.update { false }
                _uiState.value = GroupDetailsUiState.Error(
                    e.message ?: "Failed to delete group. Please try again.",
                )
            }
        }
    }

    /** Call after the deletion navigation side-effect has been consumed. */
    fun onDeletedConsumed() {
        _groupDeleted.update { false }
    }
}
