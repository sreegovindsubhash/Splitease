package com.splitease.util

import com.splitease.domain.model.Expense
import com.splitease.domain.model.ExpenseCategory
import com.splitease.domain.model.SplitMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [ExpenseCsvExporter].
 *
 * No Android dependencies — runs on the JVM.
 * Tests cover: header, ordering, amount formatting, currency, CSV escaping,
 * empty list, member name resolution, and Long minor-unit precision.
 */
class ExpenseCsvExporterTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun expense(
        id: Long,
        description: String = "Lunch",
        amountMinorUnits: Long = 60_000L,
        paidByMemberId: Long = 1L,
        date: Long = 1_000_000L,
        currencyCode: String = "INR",
    ) = Expense(
        id = id,
        groupId = 1L,
        description = description,
        amountMinorUnits = amountMinorUnits,
        currencyCode = currencyCode,
        paidByMemberId = paidByMemberId,
        category = ExpenseCategory.OTHER,
        date = date,
        splitMethod = SplitMethod.EQUAL,
    )

    /** Splits a CSV output into trimmed, non-empty lines. */
    private fun lines(csv: String) = csv.trim().lines()

    /** Parses a single (non-header) data row into its five fields, respecting quoted commas. */
    private fun parseRow(line: String): List<String> {
        val fields = mutableListOf<String>()
        var i = 0
        while (i < line.length) {
            if (line[i] == '"') {
                // Quoted field
                val sb = StringBuilder()
                i++ // skip opening quote
                while (i < line.length) {
                    if (line[i] == '"' && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i += 2
                    } else if (line[i] == '"') {
                        i++ // skip closing quote
                        break
                    } else {
                        sb.append(line[i++])
                    }
                }
                fields.add(sb.toString())
                if (i < line.length && line[i] == ',') i++ // skip separator
            } else {
                // Unquoted field
                val end = line.indexOf(',', i)
                if (end < 0) {
                    fields.add(line.substring(i))
                    break
                } else {
                    fields.add(line.substring(i, end))
                    i = end + 1
                }
            }
        }
        return fields
    }

    // ── Test 1: Header is correct ─────────────────────────────────────────────

    @Test
    fun header_isCorrect() {
        val csv = ExpenseCsvExporter.generate(emptyList(), emptyMap(), "INR")
        val firstLine = lines(csv).first()
        assertEquals("Date,Expense,Paid By,Amount,Currency,Category", firstLine)
    }

    // ── Test 2: One expense exports correctly ─────────────────────────────────

    @Test
    fun singleExpense_allFieldsCorrect() {
        // 2024-01-15 in UTC epoch millis: 1705276800000
        val dateMillis = 1_705_276_800_000L
        val exp = expense(id = 1L, description = "Dinner", amountMinorUnits = 45_050L,
            paidByMemberId = 7L, date = dateMillis)
        val memberNames = mapOf(7L to "Alice")

        val csv = ExpenseCsvExporter.generate(listOf(exp), memberNames, "INR")
        val rows = lines(csv)
        assertEquals(2, rows.size, "Should have header + 1 data row")

        val fields = parseRow(rows[1])
        assertEquals(6, fields.size)
        // Date is ISO: verify format
        assertTrue(fields[0].matches(Regex("\\d{4}-\\d{2}-\\d{2}")), "Date must be yyyy-MM-dd")
        assertEquals("Dinner", fields[1])
        assertEquals("Alice", fields[2])
        assertEquals("450.50", fields[3])
        assertEquals("INR", fields[4])
        assertEquals("Other", fields[5])
    }

    // ── Test 3: Multiple expenses are newest-first ────────────────────────────

    @Test
    fun multipleExpenses_newestFirst() {
        val older  = expense(id = 1L, description = "Old",  date = 1_000L)
        val middle = expense(id = 2L, description = "Mid",  date = 2_000L)
        val newest = expense(id = 3L, description = "New",  date = 3_000L)
        // Pass in oldest-first order; exporter must reorder.
        val csv = ExpenseCsvExporter.generate(
            listOf(older, middle, newest), mapOf(1L to "Alice"), "INR",
        )
        val rows = lines(csv)
        assertEquals(4, rows.size, "Header + 3 data rows")
        assertEquals("New",  parseRow(rows[1])[1])
        assertEquals("Mid",  parseRow(rows[2])[1])
        assertEquals("Old",  parseRow(rows[3])[1])
    }

    // ── Test 4: Amount formatting — 2-decimal currency ────────────────────────

    @Test
    fun amountFormatting_twoDecimalCurrency() {
        assertEquals("600.00",  ExpenseCsvExporter.formatAmount(60_000L, "INR"))
        assertEquals("6.00",    ExpenseCsvExporter.formatAmount(600L,    "INR"))
        assertEquals("0.01",    ExpenseCsvExporter.formatAmount(1L,      "INR"))
        assertEquals("100.50",  ExpenseCsvExporter.formatAmount(10_050L, "USD"))
        assertEquals("0.00",    ExpenseCsvExporter.formatAmount(0L,      "INR"))
    }

    // ── Test 5: Currency in CSV matches group currency ────────────────────────

    @Test
    fun currency_matchesGroupCurrency() {
        val exp = expense(id = 1L, currencyCode = "USD", amountMinorUnits = 1_000L)
        val csv = ExpenseCsvExporter.generate(listOf(exp), mapOf(1L to "Bob"), "USD")
        val fields = parseRow(lines(csv)[1])
        assertEquals("USD", fields[4])
        assertEquals("10.00", fields[3])
    }

    // ── Test 6: Expense name containing a comma is escaped ───────────────────

    @Test
    fun escapesCommaInExpenseName() {
        // "Dinner, family" contains a comma — must be quoted.
        val exp = expense(id = 1L, description = "Dinner, family")
        val csv = ExpenseCsvExporter.generate(listOf(exp), mapOf(1L to "Alice"), "INR")
        val fields = parseRow(lines(csv)[1])
        assertEquals("Dinner, family", fields[1],
            "Comma in expense name must be preserved after round-trip parsing")
    }

    @Test
    fun escapesCommaInMemberName() {
        val exp = expense(id = 1L, paidByMemberId = 2L)
        val csv = ExpenseCsvExporter.generate(
            listOf(exp), mapOf(2L to "Smith, John"), "INR",
        )
        val fields = parseRow(lines(csv)[1])
        assertEquals("Smith, John", fields[2])
    }

    // ── Test 7: Expense name containing a quote is escaped ───────────────────

    @Test
    fun escapesDoubleQuoteInExpenseName() {
        // Dinner "special" → CSV: "Dinner ""special"""
        val exp = expense(id = 1L, description = "Dinner \"special\"")
        val csv = ExpenseCsvExporter.generate(listOf(exp), mapOf(1L to "Alice"), "INR")
        val fields = parseRow(lines(csv)[1])
        assertEquals("Dinner \"special\"", fields[1],
            "Double-quoted expense name must survive round-trip parsing")
    }

    @Test
    fun csvEscape_quoteInValue_doubledAndWrapped() {
        val escaped = ExpenseCsvExporter.csvEscape("say \"hello\"")
        assertEquals("\"say \"\"hello\"\"\"", escaped)
    }

    // ── Test 8: Newlines are escaped ─────────────────────────────────────────

    @Test
    fun escapesNewlineInExpenseName() {
        val exp = expense(id = 1L, description = "Line1\nLine2")
        val csv = ExpenseCsvExporter.generate(listOf(exp), mapOf(1L to "Alice"), "INR")
        assertTrue(
            csv.contains("\"Line1\nLine2\""),
            "Multiline expense name must remain inside a quoted CSV field",
        )
    }

    @Test
    fun csvEscape_newlineInValue_wrapsInQuotes() {
        val escaped = ExpenseCsvExporter.csvEscape("a\nb")
        assertEquals("\"a\nb\"", escaped)
    }

    // ── Test 9: Long names are preserved ─────────────────────────────────────

    @Test
    fun longExpenseName_preserved() {
        val longName = "A very long expense description that exceeds any normal UI truncation limit and should pass through completely unmodified"
        val exp = expense(id = 1L, description = longName)
        val csv = ExpenseCsvExporter.generate(listOf(exp), mapOf(1L to "Alice"), "INR")
        val fields = parseRow(lines(csv)[1])
        assertEquals(longName, fields[1])
    }

    @Test
    fun longMemberName_preserved() {
        val longName = "Thiruvenkatam Subramaniam Raghunathan Krishnamurthy"
        val exp = expense(id = 1L, paidByMemberId = 5L)
        val csv = ExpenseCsvExporter.generate(listOf(exp), mapOf(5L to longName), "INR")
        val fields = parseRow(lines(csv)[1])
        assertEquals(longName, fields[2])
    }

    // ── Test 10: Empty expense list ───────────────────────────────────────────

    @Test
    fun emptyExpenses_onlyHeaderRow() {
        val csv = ExpenseCsvExporter.generate(emptyList(), emptyMap(), "INR")
        val rows = lines(csv)
        assertEquals(1, rows.size, "Empty export must contain exactly the header row")
        assertEquals("Date,Expense,Paid By,Amount,Currency,Category", rows[0])
    }

    // ── Category column ───────────────────────────────────────────────────────

    @Test
    fun categoryColumn_isIncluded() {
        val exp = expense(id = 1L)
        val csv = ExpenseCsvExporter.generate(listOf(exp), mapOf(1L to "Alice"), "INR")
        val fields = parseRow(lines(csv)[1])
        assertEquals(6, fields.size, "Row must have 6 fields including Category")
        assertEquals("Other", fields[5], "Category column must be the 6th field")
    }

    @Test
    fun categoryColumn_allCategories() {
        // Verify every ExpenseCategory produces the expected display name in the CSV
        val expenseCategories = listOf(
            ExpenseCategory.FOOD to "Food",
            ExpenseCategory.TRANSPORT to "Transport",
            ExpenseCategory.ACCOMMODATION to "Accommodation",
            ExpenseCategory.SHOPPING to "Shopping",
            ExpenseCategory.ENTERTAINMENT to "Entertainment",
            ExpenseCategory.BILLS to "Bills",
            ExpenseCategory.OTHER to "Other",
        )
        expenseCategories.forEach { (cat, expected) ->
            val exp = Expense(
                id = 1L, groupId = 1L, description = "X",
                amountMinorUnits = 100L, currencyCode = "INR",
                paidByMemberId = 1L, category = cat, date = 1_000L,
                splitMethod = SplitMethod.EQUAL,
            )
            val csv = ExpenseCsvExporter.generate(listOf(exp), mapOf(1L to "Alice"), "INR")
            val fields = parseRow(lines(csv)[1])
            assertEquals(expected, fields[5], "Category $cat should produce display name $expected")
        }
    }

    // ── Test 11: Multiple payers resolve names correctly ─────────────────────

    @Test
    fun multipleMembers_payerNamesResolvedCorrectly() {
        val exp1 = expense(id = 1L, description = "Lunch",  paidByMemberId = 1L, date = 2_000L)
        val exp2 = expense(id = 2L, description = "Dinner", paidByMemberId = 2L, date = 1_000L)
        val memberNames = mapOf(1L to "Alice", 2L to "Bob")

        val csv = ExpenseCsvExporter.generate(listOf(exp1, exp2), memberNames, "INR")
        val rows = lines(csv)
        // Newest first: Lunch (date=2000) before Dinner (date=1000)
        assertEquals("Alice", parseRow(rows[1])[2])
        assertEquals("Bob",   parseRow(rows[2])[2])
    }

    @Test
    fun unknownPayer_fallsBackToUnknown() {
        val exp = expense(id = 1L, paidByMemberId = 99L)
        val csv = ExpenseCsvExporter.generate(listOf(exp), emptyMap(), "INR")
        assertEquals("Unknown", parseRow(lines(csv)[1])[2])
    }

    // ── Test 12: Long minor-unit amounts without Double/Float ─────────────────

    @Test
    fun longMinorUnitAmount_exactFormatting() {
        // 999_999_999_999 paise = ₹9,999,999,999.99 — would lose precision as Double
        val amount = 999_999_999_999L
        assertEquals("9999999999.99", ExpenseCsvExporter.formatAmount(amount, "INR"))
    }

    @Test
    fun longMinorUnitAmount_roundTrip_noLostPrecision() {
        val amount = 100_000_000_001L   // 1,000,000,000.01
        val formatted = ExpenseCsvExporter.formatAmount(amount, "INR")
        assertEquals("1000000000.01", formatted)
    }

    // ── Bonus: zero-decimal currency ─────────────────────────────────────────

    @Test
    fun zeroDecimalCurrency_noDecimalPoint() {
        // JPY has 0 fraction digits
        assertEquals("1500", ExpenseCsvExporter.formatAmount(1500L, "JPY"))
    }

    // ── Bonus: csvEscape — no-op for plain values ─────────────────────────────

    @Test
    fun csvEscape_plainValue_unchanged() {
        assertEquals("Hello", ExpenseCsvExporter.csvEscape("Hello"))
        assertEquals("Abhirami", ExpenseCsvExporter.csvEscape("Abhirami"))
        assertEquals("600.00", ExpenseCsvExporter.csvEscape("600.00"))
    }
}
