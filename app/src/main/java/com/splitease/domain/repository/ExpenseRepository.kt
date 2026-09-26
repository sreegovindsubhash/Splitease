package com.splitease.domain.repository

import com.splitease.domain.model.Expense
import com.splitease.domain.model.ExpenseSplit
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getExpensesForGroup(groupId: Long): Flow<List<Expense>>
    suspend fun getExpenseById(id: Long): Expense?
    fun getSplitsForExpense(expenseId: Long): Flow<List<ExpenseSplit>>
    suspend fun addExpense(expense: Expense, splits: List<ExpenseSplit>): Long
    suspend fun updateExpense(expense: Expense, splits: List<ExpenseSplit>)
    suspend fun deleteExpense(expense: Expense)
}
