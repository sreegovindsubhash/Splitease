package com.splitease.domain.usecase

import com.splitease.domain.model.ExpenseSplit
import com.splitease.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow

/** Returns a live [Flow] of splits for [expenseId]. */
class GetSplitsForExpenseUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(expenseId: Long): Flow<List<ExpenseSplit>> =
        repository.getSplitsForExpense(expenseId)
}
