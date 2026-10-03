package com.splitease.presentation.screens.groupDetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.splitease.domain.repository.ExpenseRepository
import com.splitease.domain.repository.GroupRepository
import com.splitease.domain.usecase.DeleteGroupUseCase
import com.splitease.domain.usecase.SetGroupBudgetUseCase

class GroupDetailsViewModelFactory(
    private val groupRepository: GroupRepository,
    private val expenseRepository: ExpenseRepository,
    private val groupId: Long,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupDetailsViewModel::class.java)) {
            return GroupDetailsViewModel(
                groupRepository = groupRepository,
                expenseRepository = expenseRepository,
                deleteGroupUseCase = DeleteGroupUseCase(groupRepository),
                setGroupBudgetUseCase = SetGroupBudgetUseCase(groupRepository),
                groupId = groupId,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
