package com.splitease.util

import com.splitease.domain.model.Expense
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency

/**
 * Converts a list of [Expense] domain objects into a UTF-8 CSV string.
 *
 * Columns (in order):
 *   Date, Expense, Paid By, Amount, Currency
 *
 * Rules:
 *   - Header row always included.
 *   - Expenses are written newest-first (sorted by [Expense.date] descending).
 *   - Amount is formatted using integer arithmetic only — no Double/Float.
 *   - Fields containing commas, double-quotes, or newlines are quoted per RFC 4180.
 *   - Encoding is UTF-8 (callers must use UTF-8 when writing to an OutputStream).
 *
 * This object has no Android dependencies and is unit-testable on the JVM.
 */
object ExpenseCsvExporter {

    private val DATE_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())

    private val HEADER = "Date,Expense,Paid By,Amount,Currency"

    /**
     * Generates the full CSV text for the given [expenses].
     *
     * @param expenses      All expenses for the group (any order — sorted internally).
     * @param memberNames   Map of memberId → display name; used to resolve the payer column.
     * @param currencyCode  ISO 4217 currency code for the group (e.g. "INR", "USD").
     * @return              A UTF-8 CSV string including the header row.
     */
    fun generate(
        expenses: List<Expense>,
        memberNames: Map<Long, String>,
        currencyCode: String,
    ): String {
        val sb = StringBuilder()
        sb.appendLine(HEADER)

        // Sort newest-first — consistent with the Expenses screen ordering.
        val sorted = expenses.sortedByDescending { it.date }

        for (expense in sorted) {
            val date = formatDate(expense.date)
            val description = csvEscape(expense.description)
            val payerName = csvEscape(memberNames[expense.paidByMemberId] ?: "Unknown")
            val amount = formatAmount(expense.amountMinorUnits, currencyCode)
            val currency = csvEscape(currencyCode)
            sb.appendLine("$date,$description,$payerName,$amount,$currency")
        }

        // appendLine adds a trailing newline after the last row; trim it so the
        // file ends cleanly after the last data line.
        return sb.toString().trimEnd('\n', '\r')
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Formats a Unix epoch milliseconds timestamp as "yyyy-MM-dd".
     */
    internal fun formatDate(epochMillis: Long): String =
        DATE_FORMATTER.format(Instant.ofEpochMilli(epochMillis))

    /**
     * Formats [amountMinorUnits] as a plain decimal string without a currency symbol,
     * using integer arithmetic only (no Double/Float).
     *
     * Examples (INR, 2 fraction digits):
     *   60000  → "600.00"
     *   60001  → "600.01"
     *   600    → "6.00"
     *
     * Examples (JPY, 0 fraction digits):
     *   1000   → "1000"
     */
    internal fun formatAmount(amountMinorUnits: Long, currencyCode: String): String {
        val fractionDigits = try {
            Currency.getInstance(currencyCode).defaultFractionDigits
        } catch (_: IllegalArgumentException) {
            2 // safe fallback
        }
        return if (fractionDigits == 0) {
            amountMinorUnits.toString()
        } else {
            val divisor = pow10(fractionDigits)
            val major = amountMinorUnits / divisor
            val minor = amountMinorUnits % divisor
            "$major.${minor.toString().padStart(fractionDigits, '0')}"
        }
    }

    /**
     * Escapes a single CSV field per RFC 4180:
     *   - If the value contains a comma, double-quote, or newline character,
     *     wrap it in double-quotes and double any embedded double-quotes.
     *   - Otherwise return the value unchanged.
     */
    internal fun csvEscape(value: String): String {
        val needsQuoting = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuoting) {
            // Double every double-quote, then wrap the whole field in double-quotes.
            '"' + value.replace("\"", "\"\"") + '"'
        } else {
            value
        }
    }

    private fun pow10(exp: Int): Long {
        var result = 1L
        repeat(exp) { result *= 10 }
        return result
    }
}
