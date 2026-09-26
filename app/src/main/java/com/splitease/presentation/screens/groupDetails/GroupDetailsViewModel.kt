package com.splitease.presentation.screens.groupDetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splitease.domain.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class GroupDetailsViewModel(
    private val groupRepository: GroupRepository,
    private val groupId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GroupDetailsUiState>(GroupDetailsUiState.Loading)
    val uiState: StateFlow<GroupDetailsUiState> = _uiState.asStateFlow()

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
}
