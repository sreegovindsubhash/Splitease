package com.splitease.presentation.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.splitease.domain.repository.GroupRepository

class GroupsViewModelFactory(
    private val groupRepository: GroupRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupsViewModel::class.java)) {
            return GroupsViewModel(groupRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
