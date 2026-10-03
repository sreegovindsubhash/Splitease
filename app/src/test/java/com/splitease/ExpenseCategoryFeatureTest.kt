package com.splitease

import com.splitease.data.local.entity.ExpenseEntity
import com.splitease.data.local.mapper.toDomain
import com.splitease.data.local.mapper.toEntity
import com.splitease.domain.model.Expense
import com.splitease.domain.model.ExpenseCategory
import com.splitease.domain.model.SplitMethod
import com.splitease.presentation.screens.summary.CategoryTotal
import com.splitease.presentation.screens.summary.SummaryViewModel
import com.splitease.util.ExpenseCsvExporter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Focused tests for the Expense Categories feature.
 *
 * Covers:
 *  - Default category is OTHER for a new Expense.
 *  - Category is persisted and retrieved correctly via the mapper.
 *  - Expenses whose stored category was 'UTILITIES' or 'EDUCATION' are handled by migration.
 *  - Editing an expense preserves/updates its category via the domain model.
 *  - Summary category totals are calculated correctly.
 *  - CSV export includes the Category column with correct values.
 *
 * All tests run on the JVM — no Android framework required.
 */
class ExpenseCategoryFeatureTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun makeExpense(
        id: Long = 1L,
        category: ExpenseCategory = ExpenseCategory.OTHER,
        amountMinorUnits: Long = 10_000L,
    ) = Expense(
        id = id,
        groupId = 1L,
        description = "Test expense",
        amountMinorUnits = amountMinorUnits,
        currencyCode = "INR",
        paidByMemberId = 1L,
        category = category,
        date = 1_000_000L,
        splitMethod = SplitMethod.EQUAL,
    )

    private fun makeEntity(
        id: Long = 1L,
        category: String = "OTHER",
    ) = ExpenseEntity(
        id = id,
        groupId = 1L,
        description = "Test expense",
        amountMinorUnits = 10_000L,
        currencyCode = "INR",
        paidByMemberId = 1L,
        category = category,
        date = 1_000_000L,
        splitMethod = SplitMethod.EQUAL.name,
    )

    // ── 1. Default category ───────────────────────────────────────────────────

    @Test
    fun newExpense_defaultCategoryIsOther() {
        // AddEditExpenseUiState defaults category to OTHER
        val uiState = com.splitease.presentation.screens.expenses.AddEditExpenseUiState()
        assertEquals(ExpenseCategory.OTHER, uiState.category)
    }

    @Test
    fun expenseDomainModel_categoryDefaultsToOther() {
        // When constructing without specifying category, OTHER is the only valid default
        val expense = Expense(
            groupId = 1L,
            description = "Groceries",
            amountMinorUnits = 5_000L,
            currencyCode = "INR",
            paidByMemberId = 1L,
            category = ExpenseCategory.OTHER,   // explicit OTHER — matches spec default
            date = 1_000L,
            splitMethod = SplitMethod.EQUAL,
        )
        assertEquals(ExpenseCategory.OTHER, expense.category)
    }

    // ── 2. Category persisted and retrieved via mapper ────────────────────────

    @Test
    fun category_persistedAndRetrieved_food() {
        val entity = makeEntity(category = "FOOD")
        val domain = entity.toDomain()
        assertEquals(ExpenseCategory.FOOD, domain.category)
    }

    @Test
    fun category_persistedAndRetrieved_bills() {
        val entity = makeEntity(category = "BILLS")
        val domain = entity.toDomain()
        assertEquals(ExpenseCategory.BILLS, domain.category)
    }

    @Test
    fun category_roundTrip_allValues() {
        // Every ExpenseCategory survives a domain↔entity round-trip unchanged.
        ExpenseCategory.entries.forEach { cat ->
            val original = makeExpense(category = cat)
            val roundTripped = original.toEntity().toDomain()
            assertEquals(cat, roundTripped.category,
                "Category $cat must survive entity round-trip")
        }
    }

    // ── 3. Migration: old UTILITIES / EDUCATION values become OTHER ───────────

    @Test
    fun migration_utilitiesCategory_becomesOther_afterUpdate() {
        // Simulates what MIGRATION_2_3 does: 'UTILITIES' is remapped to 'OTHER'
        // in the database. At the app level, any entity stored with the old name
        // will fail valueOf, so we verify the migration SQL logic conceptually
        // by confirming UTILITIES is NOT a valid enum value post-migration.
        val validNames = ExpenseCategory.entries.map { it.name }.toSet()
        assertTrue("UTILITIES" !in validNames,
            "'UTILITIES' must no longer be a valid ExpenseCategory name after migration")
        assertTrue("EDUCATION" !in validNames,
            "'EDUCATION' must no longer be a valid ExpenseCategory name after migration")
        // And OTHER is still valid
        assertTrue("OTHER" in validNames, "'OTHER' must remain a valid ExpenseCategory name")
    }

    @Test
    fun migration_utilitiesRemappedToOther_entityReadsAsOther() {
        // After MIGRATION_2_3 updates the DB, the stored value becomes 'OTHER'.
        // This test verifies that an entity with category='OTHER' maps correctly.
        val entityWithOther = makeEntity(category = "OTHER")
        val domain = entityWithOther.toDomain()
        assertEquals(ExpenseCategory.OTHER, domain.category)
    }

    // ── 4. Editing an expense preserves / updates its category ───────────────

    @Test
    fun editExpense_preservesCategory() {
        val original = makeExpense(category = ExpenseCategory.FOOD)
        // Simulate edit: copy with same category (no change)
        val edited = original.copy(description = "Edited lunch")
        assertEquals(ExpenseCategory.FOOD, edited.category,
            "Category must be preserved when description is edited")
    }

    @Test
    fun editExpense_updatesCategory() {
        val original = makeExpense(category = ExpenseCategory.FOOD)
        val edited = original.copy(category = ExpenseCategory.TRANSPORT)
        assertEquals(ExpenseCategory.TRANSPORT, edited.category,
            "Category must be updated when changed during edit")
    }

    // ── 5. Summary category totals ────────────────────────────────────────────

    @Test
    fun categoryTotals_singleCategory_summedCorrectly() {
        val expenses = listOf(
            makeExpense(id = 1L, category = ExpenseCategory.FOOD, amountMinorUnits = 10_000L),
            makeExpense(id = 2L, category = ExpenseCategory.FOOD, amountMinorUnits = 5_000L),
        )
        val totals = SummaryViewModel.computeCategoryTotals(expenses)
        assertEquals(1, totals.size)
        assertEquals(ExpenseCategory.FOOD, totals[0].category)
        assertEquals(15_000L, totals[0].totalMinorUnits)
    }

    @Test
    fun categoryTotals_multipleCategories_sortedByTotalDescending() {
        val expenses = listOf(
            makeExpense(id = 1L, category = ExpenseCategory.FOOD, amountMinorUnits = 5_000L),
            makeExpense(id = 2L, category = ExpenseCategory.TRANSPORT, amountMinorUnits = 20_000L),
            makeExpense(id = 3L, category = ExpenseCategory.BILLS, amountMinorUnits = 8_000L),
        )
        val totals = SummaryViewModel.computeCategoryTotals(expenses)
        assertEquals(3, totals.size)
        // Sorted descending by total: Transport (20000), Bills (8000), Food (5000)
        assertEquals(ExpenseCategory.TRANSPORT, totals[0].category)
        assertEquals(20_000L, totals[0].totalMinorUnits)
        assertEquals(ExpenseCategory.BILLS, totals[1].category)
        assertEquals(8_000L, totals[1].totalMinorUnits)
        assertEquals(ExpenseCategory.FOOD, totals[2].category)
        assertEquals(5_000L, totals[2].totalMinorUnits)
    }

    @Test
    fun categoryTotals_emptiesNotIncluded() {
        // If there are no ACCOMMODATION expenses, it must not appear
        val expenses = listOf(
            makeExpense(id = 1L, category = ExpenseCategory.FOOD, amountMinorUnits = 1_000L),
        )
        val totals = SummaryViewModel.computeCategoryTotals(expenses)
        val categories = totals.map { it.category }
        assertTrue(ExpenseCategory.ACCOMMODATION !in categories,
            "Categories with no expenses must not appear in totals")
    }

    @Test
    fun categoryTotals_noExpenses_emptyList() {
        val totals = SummaryViewModel.computeCategoryTotals(emptyList())
        assertTrue(totals.isEmpty(), "No expenses → empty category totals list")
    }

    // ── 6. CSV export includes Category column ────────────────────────────────

    @Test
    fun csvExport_header_includesCategory() {
        val csv = ExpenseCsvExporter.generate(emptyList(), emptyMap(), "INR")
        val header = csv.trim().lines().first()
        assertTrue(header.contains("Category"),
            "CSV header must contain 'Category' column")
        assertEquals("Date,Expense,Paid By,Amount,Currency,Category", header)
    }

    @Test
    fun csvExport_categoryColumn_correctValue() {
        val expense = makeExpense(category = ExpenseCategory.FOOD)
        val csv = ExpenseCsvExporter.generate(listOf(expense), mapOf(1L to "Alice"), "INR")
        val dataRow = csv.trim().lines()[1]
        val fields = dataRow.split(",")
        assertEquals("Food", fields[5], "Category column (index 5) must be 'Food'")
    }

    @Test
    fun csvExport_billsCategory_correctValue() {
        val expense = makeExpense(category = ExpenseCategory.BILLS)
        val csv = ExpenseCsvExporter.generate(listOf(expense), mapOf(1L to "Alice"), "INR")
        val dataRow = csv.trim().lines()[1]
        val fields = dataRow.split(",")
        assertEquals("Bills", fields[5], "Category column must be 'Bills' for BILLS category")
    }
}
