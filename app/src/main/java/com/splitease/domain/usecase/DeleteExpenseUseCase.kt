package com.splitease.domain.usecase

import com.splitease.domain.model.Expense
import com.splitease.domain.repository.ExpenseRepository

/** Deletes an expense. Cascade handles splits via the database foreign key. */
class DeleteExpenseUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(expense: Expense) = repository.deleteExpense(expense)
}
