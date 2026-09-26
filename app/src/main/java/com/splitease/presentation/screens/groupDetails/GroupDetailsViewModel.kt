package com.splitease.presentation.screens.groupDetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splitease.domain.usecase.GetGroupByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupDetailsViewModel(
    private val getGroupByIdUseCase: GetGroupByIdUseCase,
    private val groupId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow<GroupDetailsUiState>(GroupDetailsUiState.Loading)
    val uiState: StateFlow<GroupDetailsUiState> = _uiState.asStateFlow()

    init {
        loadGroup()
    }

    fun retry() {
        _uiState.value = GroupDetailsUiState.Loading
        loadGroup()
    }

    private fun loadGroup() {
        viewModelScope.launch {
            try {
                val group = getGroupByIdUseCase(groupId)
                _uiState.value = if (group != null) {
                    GroupDetailsUiState.Success(group)
                } else {
                    GroupDetailsUiState.NotFound
                }
            } catch (e: Exception) {
                _uiState.value = GroupDetailsUiState.Error(
                    e.message ?: "Failed to load group. Please try again.",
                )
            }
        }
    }
}
