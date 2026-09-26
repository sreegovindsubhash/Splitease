package com.splitease.domain.usecase

import com.splitease.domain.model.Expense
import com.splitease.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow

/** Returns a live [Flow] of expenses for [groupId], ordered by date descending. */
class GetExpensesForGroupUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(groupId: Long): Flow<List<Expense>> =
        repository.getExpensesForGroup(groupId)
}
