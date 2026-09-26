package com.splitease.domain.usecase

import com.splitease.domain.model.Expense
import com.splitease.domain.repository.ExpenseRepository

/** Returns the [Expense] with the given [id], or null if it does not exist. */
class GetExpenseByIdUseCase(private val repository: ExpenseRepository) {
    suspend operator fun invoke(id: Long): Expense? = repository.getExpenseById(id)
}
