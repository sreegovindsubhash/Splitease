package com.splitease.data.local.mapper

import com.splitease.data.local.entity.ExpenseEntity
import com.splitease.data.local.entity.ExpenseSplitEntity
import com.splitease.domain.model.Expense
import com.splitease.domain.model.ExpenseCategory
import com.splitease.domain.model.ExpenseSplit
import com.splitease.domain.model.SplitMethod
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpenseMapperTest {

    private fun sampleExpenseEntity() = ExpenseEntity(
        id = 10L,
        groupId = 1L,
        description = "Dinner",
        amountMinorUnits = 150_00L, // ₹150.00
        currencyCode = "INR",
        paidByMemberId = 2L,
        category = ExpenseCategory.FOOD.name,
        date = 1_700_000_000L,
        note = "Pizza",
        splitMethod = SplitMethod.EQUAL.name,
        createdAt = 100L,
        updatedAt = 200L,
    )

    @Test
    fun `ExpenseEntity toDomain preserves amountMinorUnits as Long`() {
        val domain = sampleExpenseEntity().toDomain()
        // Money must remain Long — never converted through Double
        assertEquals(150_00L, domain.amountMinorUnits)
        assertEquals(ExpenseCategory.FOOD, domain.category)
        assertEquals(SplitMethod.EQUAL, domain.splitMethod)
    }

    @Test
    fun `Expense toEntity round-trip preserves all fields`() {
        val original = sampleExpenseEntity()
        val roundTripped = original.toDomain().toEntity()
        assertEquals(original, roundTripped)
    }

    @Test
    fun `ExpenseSplitEntity toDomain round-trip`() {
        val entity = ExpenseSplitEntity(
            id = 1L,
            expenseId = 10L,
            memberId = 2L,
            shareMinorUnits = 75_00L,
        )
        assertEquals(entity, entity.toDomain().toEntity())
    }

    @Test
    fun `all ExpenseCategory enum values are mappable`() {
        // Ensures valueOf will not throw for any stored value
        ExpenseCategory.entries.forEach { cat ->
            assertEquals(cat, ExpenseCategory.valueOf(cat.name))
        }
    }

    @Test
    fun `all SplitMethod enum values are mappable`() {
        SplitMethod.entries.forEach { method ->
            assertEquals(method, SplitMethod.valueOf(method.name))
        }
    }
}
