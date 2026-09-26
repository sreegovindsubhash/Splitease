package com.splitease.data.repository

import androidx.room.withTransaction
import com.splitease.data.local.database.AppDatabase
import com.splitease.data.local.mapper.toDomain
import com.splitease.data.local.mapper.toEntity
import com.splitease.domain.model.Expense
import com.splitease.domain.model.ExpenseSplit
import com.splitease.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExpenseRepositoryImpl(
    private val db: AppDatabase,
) : ExpenseRepository {

    private val expenseDao get() = db.expenseDao()
    private val splitDao get() = db.expenseSplitDao()

    override fun getExpensesForGroup(groupId: Long): Flow<List<Expense>> =
        expenseDao.getExpensesForGroup(groupId).map { list -> list.map { it.toDomain() } }

    override suspend fun getExpenseById(id: Long): Expense? =
        expenseDao.getExpenseById(id)?.toDomain()

    override fun getSplitsForExpense(expenseId: Long): Flow<List<ExpenseSplit>> =
        splitDao.getSplitsForExpense(expenseId).map { list -> list.map { it.toDomain() } }

    override fun getSplitsForGroup(groupId: Long): Flow<List<ExpenseSplit>> =
        splitDao.getSplitsForGroup(groupId).map { list -> list.map { it.toDomain() } }

    /**
     * Inserts the expense and its splits in a single transaction.
     * The splits must reconcile: sum(splits.shareMinorUnits) == expense.amountMinorUnits.
     * Validation is the caller's responsibility (use case layer).
     */
    override suspend fun addExpense(expense: Expense, splits: List<ExpenseSplit>): Long =
        db.withTransaction {
            val expenseId = expenseDao.insertExpense(expense.toEntity())
            val splitEntities = splits.map { it.copy(expenseId = expenseId).toEntity() }
            splitDao.insertSplits(splitEntities)
            expenseId
        }

    /**
     * Replaces the expense and atomically deletes then re-inserts all splits.
     * Guarantees no orphan or stale split rows.
     */
    override suspend fun updateExpense(expense: Expense, splits: List<ExpenseSplit>) =
        db.withTransaction {
            expenseDao.updateExpense(expense.toEntity())
            splitDao.deleteSplitsForExpense(expense.id)
            val splitEntities = splits.map { it.copy(expenseId = expense.id).toEntity() }
            splitDao.insertSplits(splitEntities)
        }

    /** Cascade delete handles splits via foreign key. */
    override suspend fun deleteExpense(expense: Expense) =
        expenseDao.deleteExpense(expense.toEntity())
}
