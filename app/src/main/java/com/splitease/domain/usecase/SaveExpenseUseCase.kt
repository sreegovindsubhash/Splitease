package com.splitease.domain.usecase

import com.splitease.domain.model.Expense
import com.splitease.domain.model.ExpenseSplit
import com.splitease.domain.model.SplitMethod
import com.splitease.domain.repository.ExpenseRepository

/**
 * Validates and saves a new expense together with its splits.
 *
 * Validation rules enforced here (before touching the repository):
 * - Description must not be blank.
 * - Amount must be > 0.
 * - Payer must be a valid (non-zero) member id.
 * - Splits must be non-empty.
 * - sum(split.shareMinorUnits) must equal expense.amountMinorUnits exactly.
 * - Individual shares must be >= 0.
 *
 * Returns the new expense id.
 */
class SaveExpenseUseCase(private val repository: ExpenseRepository) {

    suspend operator fun invoke(expense: Expense, splits: List<ExpenseSplit>): Long {
        validate(expense, splits)
        return repository.addExpense(expense, splits)
    }

    private fun validate(expense: Expense, splits: List<ExpenseSplit>) {
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
