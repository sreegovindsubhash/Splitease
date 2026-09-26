package com.splitease.domain.usecase

import com.splitease.domain.model.Expense
import com.splitease.domain.model.ExpenseSplit
import com.splitease.domain.repository.ExpenseRepository

/**
 * Validates and updates an existing expense.
 * Applies the same validation rules as [SaveExpenseUseCase].
 */
class UpdateExpenseUseCase(private val repository: ExpenseRepository) {

    suspend operator fun invoke(expense: Expense, splits: List<ExpenseSplit>) {
        validate(expense, splits)
        repository.updateExpense(expense, splits)
    }

    private fun validate(expense: Expense, splits: List<ExpenseSplit>) {
        require(expense.id > 0) { "Expense id must be valid for an update." }
        require(expense.description.isNotBlank()) { "Description is required." }
        require(expense.amountMinorUnits > 0) { "Amount must be greater than zero." }
        require(expense.paidByMemberId > 0) { "A payer must be selected." }
        require(splits.isNotEmpty()) { "At least one participant is required." }
        require(splits.all { it.shareMinorUnits >= 0 }) { "Split shares cannot be negative." }
        val splitTotal = splits.sumOf { it.shareMinorUnits }
        require(splitTotal == expense.amountMinorUnits) {
            "Split amounts (${splitTotal}) must reconcile exactly to the expense amount (${expense.amountMinorUnits})."
        }
    }
}
