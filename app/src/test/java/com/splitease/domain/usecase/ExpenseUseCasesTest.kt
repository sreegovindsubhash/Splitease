package com.splitease.domain.usecase

import com.splitease.domain.model.Expense
import com.splitease.domain.model.ExpenseCategory
import com.splitease.domain.model.ExpenseSplit
import com.splitease.domain.model.SplitMethod
import com.splitease.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/** Unit tests for Save/Update/Delete expense use cases. */
class ExpenseUseCasesTest {

    private val fakeRepo = FakeExpenseRepo()

    // ── SaveExpenseUseCase ────────────────────────────────────────────────────

    @Test
    fun saveExpense_valid_persists() = runTest {
        val expense = makeExpense(amountMinorUnits = 10_00L)
        val splits = listOf(split(memberId = 1L, share = 10_00L))

        val id = SaveExpenseUseCase(fakeRepo)(expense, splits)

        assertEquals(1L, id)
        assertNotNull(fakeRepo.lastSavedExpense)
        assertEquals("Lunch", fakeRepo.lastSavedExpense!!.description)
    }

    @Test
    fun saveExpense_blankDescription_throws() = runTest {
        val expense = makeExpense(description = "  ", amountMinorUnits = 10_00L)
        val splits = listOf(split(memberId = 1L, share = 10_00L))

        assertFailsWith<IllegalArgumentException> {
            SaveExpenseUseCase(fakeRepo)(expense, splits)
        }
    }

    @Test
    fun saveExpense_zeroAmount_throws() = runTest {
        val expense = makeExpense(amountMinorUnits = 0L)
        val splits = listOf(split(memberId = 1L, share = 0L))

        assertFailsWith<IllegalArgumentException> {
            SaveExpenseUseCase(fakeRepo)(expense, splits)
        }
    }

    @Test
    fun saveExpense_negativeAmount_throws() = runTest {
        val expense = makeExpense(amountMinorUnits = -100L)
        val splits = listOf(split(memberId = 1L, share = -100L))

        assertFailsWith<IllegalArgumentException> {
            SaveExpenseUseCase(fakeRepo)(expense, splits)
        }
    }

    @Test
    fun saveExpense_invalidPayer_throws() = runTest {
        val expense = makeExpense(paidByMemberId = 0L, amountMinorUnits = 10_00L)
        val splits = listOf(split(memberId = 1L, share = 10_00L))

        assertFailsWith<IllegalArgumentException> {
            SaveExpenseUseCase(fakeRepo)(expense, splits)
        }
    }

    @Test
    fun saveExpense_noParticipants_throws() = runTest {
        val expense = makeExpense(amountMinorUnits = 10_00L)

        assertFailsWith<IllegalArgumentException> {
            SaveExpenseUseCase(fakeRepo)(expense, emptyList())
        }
    }

    @Test
    fun saveExpense_splitNotReconciled_throws() = runTest {
        val expense = makeExpense(amountMinorUnits = 10_00L)
        val splits = listOf(split(memberId = 1L, share = 5_00L))   // 500 ≠ 1000

        assertFailsWith<IllegalArgumentException> {
            SaveExpenseUseCase(fakeRepo)(expense, splits)
        }
    }

    @Test
    fun saveExpense_splitExactlyReconciles() = runTest {
        // 100 split 3 ways: 34 + 33 + 33 = 100
        val expense = makeExpense(amountMinorUnits = 100L)
        val splits = listOf(
            split(memberId = 1L, share = 34L),
            split(memberId = 2L, share = 33L),
            split(memberId = 3L, share = 33L),
        )

        SaveExpenseUseCase(fakeRepo)(expense, splits)  // must not throw
        assertEquals(100L, fakeRepo.lastSavedSplits.sumOf { it.shareMinorUnits })
    }

    // ── UpdateExpenseUseCase ──────────────────────────────────────────────────

    @Test
    fun updateExpense_valid_callsRepository() = runTest {
        val expense = makeExpense(id = 5L, amountMinorUnits = 20_00L)
        val splits = listOf(split(memberId = 1L, share = 20_00L))

        UpdateExpenseUseCase(fakeRepo)(expense, splits)

        assertEquals(expense.id, fakeRepo.lastUpdatedExpense?.id)
        assertEquals(1, fakeRepo.lastUpdatedSplits.size)
    }

    @Test
    fun updateExpense_invalidId_throws() = runTest {
        val expense = makeExpense(id = 0L, amountMinorUnits = 10_00L)
        val splits = listOf(split(memberId = 1L, share = 10_00L))

        assertFailsWith<IllegalArgumentException> {
            UpdateExpenseUseCase(fakeRepo)(expense, splits)
        }
    }

    @Test
    fun updateExpense_blankDescription_throws() = runTest {
        val expense = makeExpense(id = 1L, description = "", amountMinorUnits = 10_00L)
        val splits = listOf(split(memberId = 1L, share = 10_00L))

        assertFailsWith<IllegalArgumentException> {
            UpdateExpenseUseCase(fakeRepo)(expense, splits)
        }
    }

    @Test
    fun updateExpense_splitNotReconciled_throws() = runTest {
        val expense = makeExpense(id = 1L, amountMinorUnits = 10_00L)
        val splits = listOf(split(memberId = 1L, share = 9_99L))

        assertFailsWith<IllegalArgumentException> {
            UpdateExpenseUseCase(fakeRepo)(expense, splits)
        }
    }

    // ── DeleteExpenseUseCase ──────────────────────────────────────────────────

    @Test
    fun deleteExpense_delegatesToRepository() = runTest {
        val expense = makeExpense(id = 3L, amountMinorUnits = 10_00L)
        DeleteExpenseUseCase(fakeRepo)(expense)
        assertEquals(expense, fakeRepo.lastDeleted)
    }

    // ── GetExpensesForGroupUseCase ────────────────────────────────────────────

    @Test
    fun getExpensesForGroup_returnsFlow() = runTest {
        val expense = makeExpense(id = 1L, amountMinorUnits = 50_00L)
        fakeRepo.expensesForGroup = listOf(expense)

        var result: List<Expense> = emptyList()
        GetExpensesForGroupUseCase(fakeRepo)(groupId = 1L).collect { result = it }

        assertEquals(1, result.size)
        assertEquals("Lunch", result.first().description)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun makeExpense(
    id: Long = 0L,
    description: String = "Lunch",
    amountMinorUnits: Long = 10_00L,
    paidByMemberId: Long = 1L,
) = Expense(
    id = id,
    groupId = 1L,
    description = description,
    amountMinorUnits = amountMinorUnits,
    currencyCode = "INR",
    paidByMemberId = paidByMemberId,
    category = ExpenseCategory.FOOD,
    date = System.currentTimeMillis(),
    splitMethod = SplitMethod.EQUAL,
)

private fun split(memberId: Long, share: Long) = ExpenseSplit(
    expenseId = 0L,
    memberId = memberId,
    shareMinorUnits = share,
)

private class FakeExpenseRepo : ExpenseRepository {

    var expensesForGroup: List<Expense> = emptyList()
    var lastSavedExpense: Expense? = null
    var lastSavedSplits: List<ExpenseSplit> = emptyList()
    var lastUpdatedExpense: Expense? = null
    var lastUpdatedSplits: List<ExpenseSplit> = emptyList()
    var lastDeleted: Expense? = null
    private var nextId = 1L

    override fun getExpensesForGroup(groupId: Long): Flow<List<Expense>> =
        flowOf(expensesForGroup)

    override suspend fun getExpenseById(id: Long): Expense? =
        expensesForGroup.firstOrNull { it.id == id }

    override fun getSplitsForExpense(expenseId: Long): Flow<List<ExpenseSplit>> =
        flowOf(emptyList())

    override suspend fun addExpense(expense: Expense, splits: List<ExpenseSplit>): Long {
        lastSavedExpense = expense
        lastSavedSplits = splits
        return nextId++
    }

    override suspend fun updateExpense(expense: Expense, splits: List<ExpenseSplit>) {
        lastUpdatedExpense = expense
        lastUpdatedSplits = splits
    }

    override suspend fun deleteExpense(expense: Expense) {
        lastDeleted = expense
    }
}
