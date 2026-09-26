package com.splitease.presentation.screens.createGroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.splitease.domain.repository.GroupRepository
import com.splitease.domain.usecase.CreateGroupUseCase

class CreateGroupViewModelFactory(
    private val groupRepository: GroupRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateGroupViewModel::class.java)) {
            return CreateGroupViewModel(CreateGroupUseCase(groupRepository)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
