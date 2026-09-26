package com.splitease.presentation.screens.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.splitease.domain.repository.ExpenseRepository
import com.splitease.domain.repository.GroupRepository
import com.splitease.domain.repository.MemberRepository
import com.splitease.domain.usecase.DeleteExpenseUseCase
import com.splitease.domain.usecase.GetExpensesForGroupUseCase

class ExpensesViewModelFactory(
    private val groupId: Long,
    private val expenseRepository: ExpenseRepository,
    private val groupRepository: GroupRepository,
    private val memberRepository: MemberRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpensesViewModel::class.java)) {
            return ExpensesViewModel(
                groupId = groupId,
                getExpensesForGroupUseCase = GetExpensesForGroupUseCase(expenseRepository),
                deleteExpenseUseCase = DeleteExpenseUseCase(expenseRepository),
                groupRepository = groupRepository,
                memberRepository = memberRepository,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
