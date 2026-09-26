package com.splitease.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {

    @Test
    fun `format INR paise to rupees`() {
        assertEquals("₹100.50", MoneyFormatter.format(10050, "INR"))
    }

    @Test
    fun `format INR whole rupees`() {
        assertEquals("₹5.00", MoneyFormatter.format(500, "INR"))
    }

    @Test
    fun `format zero amount`() {
        assertEquals("₹0.00", MoneyFormatter.format(0, "INR"))
    }

    @Test
    fun `format USD cents`() {
        val result = MoneyFormatter.format(1234, "USD")
        // Symbol varies by locale but structure should include 12.34
        assert(result.contains("12.34")) { "Expected 12.34 in result '$result'" }
    }
}
