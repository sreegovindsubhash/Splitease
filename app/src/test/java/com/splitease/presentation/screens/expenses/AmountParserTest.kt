package com.splitease.presentation.screens.expenses

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Tests for the pure helper functions on AddEditExpenseViewModel companion object. */
class AmountParserTest {

    // ── parseAmount ───────────────────────────────────────────────────────────

    @Test
    fun parseAmount_integer_convertsToMinorUnits() {
        assertEquals(10_000L, AddEditExpenseViewModel.parseAmount("100"))
    }

    @Test
    fun parseAmount_decimal_twoPlaces() {
        assertEquals(10_050L, AddEditExpenseViewModel.parseAmount("100.50"))
    }

    @Test
    fun parseAmount_decimal_onePlaceIsPaddedRight() {
        // "100.5" → 10050 (not 10005)
        assertEquals(10_050L, AddEditExpenseViewModel.parseAmount("100.5"))
    }

    @Test
    fun parseAmount_zero_returnsZero() {
        assertEquals(0L, AddEditExpenseViewModel.parseAmount("0"))
    }

    @Test
    fun parseAmount_blank_returnsNull() {
        assertNull(AddEditExpenseViewModel.parseAmount(""))
        assertNull(AddEditExpenseViewModel.parseAmount("   "))
    }

    @Test
    fun parseAmount_nonNumeric_returnsNull() {
        assertNull(AddEditExpenseViewModel.parseAmount("abc"))
        assertNull(AddEditExpenseViewModel.parseAmount("12.ab"))
    }

    @Test
    fun parseAmount_commaDecimalSeparator_isHandled() {
        assertEquals(10_050L, AddEditExpenseViewModel.parseAmount("100,50"))
    }

    @Test
    fun parseAmount_negative_returnsNull() {
        assertNull(AddEditExpenseViewModel.parseAmount("-100"))
    }

    // ── parseBasisPoints ──────────────────────────────────────────────────────

    @Test
    fun parseBasisPoints_hundred_is10000() {
        assertEquals(10_000L, AddEditExpenseViewModel.parseBasisPoints("100"))
    }

    @Test
    fun parseBasisPoints_decimal() {
        assertEquals(3_333L, AddEditExpenseViewModel.parseBasisPoints("33.33"))
    }

    @Test
    fun parseBasisPoints_blank_returnsNull() {
        assertNull(AddEditExpenseViewModel.parseBasisPoints(""))
    }

    @Test
    fun parseBasisPoints_invalid_returnsNull() {
        assertNull(AddEditExpenseViewModel.parseBasisPoints("xyz"))
    }

    // ── Round-trip: minorToDisplayString / parseAmount ────────────────────────

    @Test
    fun roundTrip_100paise_isStable() {
        val minor = 10_050L
        val display = AddEditExpenseViewModel.minorToDisplayString(minor, "INR")
        val back = AddEditExpenseViewModel.parseAmount(display)
        assertEquals(minor, back)
    }

    @Test
    fun roundTrip_exactInteger_isStable() {
        val minor = 5_000L
        val display = AddEditExpenseViewModel.minorToDisplayString(minor, "INR")
        val back = AddEditExpenseViewModel.parseAmount(display)
        assertEquals(minor, back)
    }
}
