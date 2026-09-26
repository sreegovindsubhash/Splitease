package com.splitease.presentation.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.splitease.domain.repository.ExpenseRepository
import com.splitease.domain.repository.GroupRepository
import com.splitease.domain.repository.MemberRepository
import com.splitease.domain.usecase.GetExpenseByIdUseCase
import com.splitease.domain.usecase.GetSplitsForExpenseUseCase
import com.splitease.domain.usecase.SaveExpenseUseCase
import com.splitease.domain.usecase.UpdateExpenseUseCase

class AddEditExpenseViewModelFactory(
    private val groupId: Long,
    private val editExpenseId: Long,
    private val expenseRepository: ExpenseRepository,
    private val groupRepository: GroupRepository,
    private val memberRepository: MemberRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEditExpenseViewModel::class.java)) {
            return AddEditExpenseViewModel(
                groupId = groupId,
                editExpenseId = editExpenseId,
                groupRepository = groupRepository,
                memberRepository = memberRepository,
                saveExpenseUseCase = SaveExpenseUseCase(expenseRepository),
                updateExpenseUseCase = UpdateExpenseUseCase(expenseRepository),
                getExpenseByIdUseCase = GetExpenseByIdUseCase(expenseRepository),
                getSplitsForExpenseUseCase = GetSplitsForExpenseUseCase(expenseRepository),
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
