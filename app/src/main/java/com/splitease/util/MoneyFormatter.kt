package com.splitease.util

import java.util.Currency
import java.util.Locale

/**
 * Formats monetary amounts stored as Long minor units (e.g. paise, cents).
 *
 * NEVER converts through Double or Float.
 * Uses integer arithmetic only to produce the display string.
 */
object MoneyFormatter {

    /**
     * Formats [amountMinorUnits] into a human-readable string using the given [currencyCode].
     *
     * For two-decimal currencies (INR, USD, EUR, GBP, etc.):
     *   10050 paise  → "₹100.50"
     *   500          → "₹5.00"
     *
     * For zero-decimal currencies (JPY, etc.):
     *   1000 yen     → "¥1,000"
     */
    fun format(amountMinorUnits: Long, currencyCode: String): String {
        return try {
            val currency = Currency.getInstance(currencyCode)
            val fractionDigits = currency.defaultFractionDigits
            val symbol = currency.symbol

            if (fractionDigits == 0) {
                "$symbol$amountMinorUnits"
            } else {
                val divisor = pow10(fractionDigits)
                val major = amountMinorUnits / divisor
                val minor = amountMinorUnits % divisor
                val minorStr = minor.toString().padStart(fractionDigits, '0')
                "$symbol$major.$minorStr"
            }
        } catch (e: IllegalArgumentException) {
            // Fallback if currency code is unrecognised
            "$currencyCode $amountMinorUnits"
        }
    }

    private fun pow10(exp: Int): Long {
        var result = 1L
        repeat(exp) { result *= 10 }
        return result
    }
}
