package com.splitease.presentation.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splitease.domain.repository.GroupRepository
import com.splitease.domain.usecase.GetGroupsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class GroupsViewModel(
    groupRepository: GroupRepository,
) : ViewModel() {

    private val getGroupsUseCase = GetGroupsUseCase(groupRepository)

    val uiState: StateFlow<GroupsUiState> = getGroupsUseCase()
        .map { groups ->
            if (groups.isEmpty()) GroupsUiState.Empty
            else GroupsUiState.Success(groups)
        }
        .catch { e -> emit(GroupsUiState.Error(e.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GroupsUiState.Loading,
        )
}
