package com.splitease.domain.usecase

import com.splitease.domain.model.Expense
import com.splitease.domain.model.ExpenseCategory
import com.splitease.domain.model.ExpenseSplit
import com.splitease.domain.model.SplitMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CalculateMemberBalancesUseCaseTest {

    private val useCase = CalculateMemberBalancesUseCase()

    private val names = mapOf(
        1L to "Alice",
        2L to "Bob",
        3L to "Charlie"
    )

    private fun expense(
        id: Long,
        amount: Long,
        paidBy: Long
    ) = Expense(
        id = id,
        groupId = 100L,
        description = "Dinner",
        amountMinorUnits = amount,
        currencyCode = "INR",
        paidByMemberId = paidBy,
        category = ExpenseCategory.FOOD,
        date = 1_700_000_000_000L,
        splitMethod = SplitMethod.EQUAL
    )

    private fun split(
        id: Long,
        expenseId: Long,
        memberId: Long,
        amount: Long
    ) = ExpenseSplit(
        id = id,
        expenseId = expenseId,
        memberId = memberId,
        shareMinorUnits = amount
    )

    @Test
    fun calculatesSimpleThreeMemberBalance() {
        val expenses = listOf(
            expense(
                id = 1L,
                amount = 900L,
                paidBy = 1L
            )
        )

        val splits = listOf(
            split(1L, 1L, 1L, 300L),
            split(2L, 1L, 2L, 300L),
            split(3L, 1L, 3L, 300L)
        )

        val result = useCase(
            groupId = 100L,
            expenses = expenses,
            splits = splits,
            memberNames = names
        )

        assertEquals(
            listOf(600L, -300L, -300L),
            result.map { it.netMinorUnits }
        )
    }

    @Test
    fun combinesMultipleExpenses() {
        val expenses = listOf(
            expense(1L, 900L, 1L),
            expense(2L, 600L, 2L)
        )

        val splits = listOf(
            split(1L, 1L, 1L, 300L),
            split(2L, 1L, 2L, 300L),
            split(3L, 1L, 3L, 300L),

            split(4L, 2L, 1L, 200L),
            split(5L, 2L, 2L, 200L),
            split(6L, 2L, 3L, 200L)
        )

        val result = useCase(
            groupId = 100L,
            expenses = expenses,
            splits = splits,
            memberNames = names
        )

        assertEquals(
            listOf(400L, 100L, -500L),
            result.map { it.netMinorUnits }
        )
    }

    @Test
    fun includesMembersWithZeroBalance() {
        val expenses = listOf(
            expense(1L, 100L, 1L)
        )

        val splits = listOf(
            split(1L, 1L, 1L, 100L)
        )

        val result = useCase(
            groupId = 100L,
            expenses = expenses,
            splits = splits,
            memberNames = names
        )

        assertEquals(
            listOf(0L, 0L, 0L),
            result.map { it.netMinorUnits }
        )
    }

    @Test
    fun ignoresExpensesFromOtherGroups() {
        val expenses = listOf(
            expense(1L, 900L, 1L),
            expense(2L, 600L, 2L).copy(groupId = 200L)
        )

        val splits = listOf(
            split(1L, 1L, 1L, 300L),
            split(2L, 1L, 2L, 300L),
            split(3L, 1L, 3L, 300L),

            split(4L, 2L, 1L, 200L),
            split(5L, 2L, 2L, 200L),
            split(6L, 2L, 3L, 200L)
        )

        val result = useCase(
            groupId = 100L,
            expenses = expenses,
            splits = splits,
            memberNames = names
        )

        assertEquals(
            listOf(600L, -300L, -300L),
            result.map { it.netMinorUnits }
        )
    }

    @Test
    fun rejectsIncorrectSplitTotal() {
        val expenses = listOf(
            expense(1L, 900L, 1L)
        )

        val splits = listOf(
            split(1L, 1L, 1L, 300L),
            split(2L, 1L, 2L, 300L)
        )

        assertFailsWith<IllegalArgumentException> {
            useCase(
                groupId = 100L,
                expenses = expenses,
                splits = splits,
                memberNames = names
            )
        }
    }

    @Test
    fun balancesAlwaysSumToZero() {
        val expenses = listOf(
            expense(1L, 900L, 1L),
            expense(2L, 600L, 2L)
        )

        val splits = listOf(
            split(1L, 1L, 1L, 300L),
            split(2L, 1L, 2L, 300L),
            split(3L, 1L, 3L, 300L),

            split(4L, 2L, 1L, 200L),
            split(5L, 2L, 2L, 200L),
            split(6L, 2L, 3L, 200L)
        )

        val result = useCase(
            groupId = 100L,
            expenses = expenses,
            splits = splits,
            memberNames = names
        )

        assertEquals(
            0L,
            result.sumOf { it.netMinorUnits }
        )
    }
}