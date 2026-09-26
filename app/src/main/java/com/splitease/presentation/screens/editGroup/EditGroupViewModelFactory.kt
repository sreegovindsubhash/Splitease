package com.splitease.presentation.screens.editGroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.splitease.domain.repository.GroupRepository
import com.splitease.domain.usecase.UpdateGroupUseCase

class EditGroupViewModelFactory(
    private val groupRepository: GroupRepository,
    private val groupId: Long,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditGroupViewModel::class.java)) {
            return EditGroupViewModel(
                groupRepository = groupRepository,
                updateGroupUseCase = UpdateGroupUseCase(groupRepository),
                groupId = groupId,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
