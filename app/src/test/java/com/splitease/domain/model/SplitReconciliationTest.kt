package com.splitease.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests that split reconciliation holds: sum(shares) == expense.amountMinorUnits.
 *
 * These test the helper functions that will be used by split use cases.
 * All arithmetic uses Long — Double/Float are never involved.
 */
class SplitReconciliationTest {

    // ── Equal split helpers ──────────────────────────────────────────────────

    /**
     * Divides [total] equally among [count] members.
     * The remainder (total % count paise) is distributed one unit at a time
     * to the first N members — fully deterministic, no floating-point.
     */
    private fun equalShares(total: Long, count: Int): List<Long> {
        require(count > 0)
        val base = total / count
        val remainder = (total % count).toInt()
        return List(count) { index -> if (index < remainder) base + 1 else base }
    }

    // ── Percentage split helpers ─────────────────────────────────────────────

    /**
     * Converts [percentagesBp] (basis points, 0..10000) to minor-unit shares.
     * Remainder distributed to the first member with the largest truncation loss.
     * All arithmetic is integer-only.
     */
    private fun percentageShares(total: Long, percentagesBp: List<Int>): List<Long> {
        require(percentagesBp.sum() == 10_000) { "Percentages must sum to 100%" }
        val raw = percentagesBp.map { bp -> total * bp / 10_000 }
        val distributed = raw.sum()
        val remainder = total - distributed
        // Give the remainder to the first slot (deterministic)
        return raw.toMutableList().also { it[0] += remainder }
    }

    // ── Shares split helpers ─────────────────────────────────────────────────

    private fun sharesSplit(total: Long, shares: List<Int>): List<Long> {
        val totalShares = shares.sum().toLong()
        require(totalShares > 0)
        val raw = shares.map { s -> total * s / totalShares }
        val distributed = raw.sum()
        val remainder = total - distributed
        return raw.toMutableList().also { it[0] += remainder }
    }

    // ── Equal split tests ────────────────────────────────────────────────────

    @Test
    fun `equal split 3 members 1900 paise reconciles`() {
        val shares = equalShares(1900L, 3)
        assertEquals(1900L, shares.sum())
        // Two members get 634, one gets 632
        assertEquals(listOf(634L, 633L, 633L), shares)
    }

    @Test
    fun `equal split 2 members even amount`() {
        val shares = equalShares(1000L, 2)
        assertEquals(1000L, shares.sum())
        assertEquals(listOf(500L, 500L), shares)
    }

    @Test
    fun `equal split 1 member gets full amount`() {
        val shares = equalShares(500L, 1)
        assertEquals(500L, shares.sum())
        assertEquals(listOf(500L), shares)
    }

    @Test
    fun `equal split zero amount`() {
        val shares = equalShares(0L, 3)
        assertEquals(0L, shares.sum())
        assertEquals(listOf(0L, 0L, 0L), shares)
    }

    @Test
    fun `equal split 5 members 103 paise remainder distributed`() {
        val shares = equalShares(103L, 5)
        assertEquals(103L, shares.sum())
        // base = 20, remainder = 3 → first 3 get 21
        assertEquals(listOf(21L, 21L, 21L, 20L, 20L), shares)
    }

    // ── Percentage split tests ───────────────────────────────────────────────

    @Test
    fun `percentage split 50-50 reconciles`() {
        val shares = percentageShares(1000L, listOf(5000, 5000))
        assertEquals(1000L, shares.sum())
        assertEquals(listOf(500L, 500L), shares)
    }

    @Test
    fun `percentage split 40-60 reconciles`() {
        val shares = percentageShares(1000L, listOf(4000, 6000))
        assertEquals(1000L, shares.sum())
    }

    @Test
    fun `percentage split 33-33-34 reconciles`() {
        val shares = percentageShares(100L, listOf(3333, 3333, 3334))
        assertEquals(100L, shares.sum())
    }

    @Test
    fun `percentage split handles remainder correctly`() {
        // 1 paise / 3 members at 33.33% each → remainder goes to first
        val shares = percentageShares(1L, listOf(3333, 3333, 3334))
        assertEquals(1L, shares.sum())
    }

    // ── Shares split tests ───────────────────────────────────────────────────

    @Test
    fun `shares split 1-2-1 reconciles`() {
        val shares = sharesSplit(1200L, listOf(1, 2, 1))
        assertEquals(1200L, shares.sum())
        assertEquals(listOf(300L, 600L, 300L), shares)
    }

    @Test
    fun `shares split with uneven total reconciles`() {
        val shares = sharesSplit(100L, listOf(1, 2, 1))
        assertEquals(100L, shares.sum())
        // base: 25, 50, 25 → sum = 100, no remainder
        assertEquals(listOf(25L, 50L, 25L), shares)
    }

    @Test
    fun `shares split 1-1-1 on 100 paise reconciles`() {
        val shares = sharesSplit(100L, listOf(1, 1, 1))
        assertEquals(100L, shares.sum())
    }

    @Test
    fun `shares split 3 members on 1 paise goes to first`() {
        val shares = sharesSplit(1L, listOf(1, 1, 1))
        assertEquals(1L, shares.sum())
    }

    // ── MemberBalance calculation ────────────────────────────────────────────

    @Test
    fun `positive net balance means member is owed money`() {
        val balance = MemberBalance(
            memberId = 1L,
            memberName = "Alice",
            totalPaidMinorUnits = 1000L,
            totalOwedMinorUnits = 500L,
        )
        assertEquals(500L, balance.netMinorUnits)
    }

    @Test
    fun `negative net balance means member owes money`() {
        val balance = MemberBalance(
            memberId = 2L,
            memberName = "Bob",
            totalPaidMinorUnits = 0L,
            totalOwedMinorUnits = 500L,
        )
        assertEquals(-500L, balance.netMinorUnits)
    }

    @Test
    fun `zero net balance means settled`() {
        val balance = MemberBalance(
            memberId = 3L,
            memberName = "Charlie",
            totalPaidMinorUnits = 500L,
            totalOwedMinorUnits = 500L,
        )
        assertEquals(0L, balance.netMinorUnits)
    }

    @Test
    fun `group net balances always sum to zero`() {
        // The sum of all net balances in a group must be 0
        val balances = listOf(
            MemberBalance(1L, "Alice", totalPaidMinorUnits = 1500L, totalOwedMinorUnits = 500L),
            MemberBalance(2L, "Bob",   totalPaidMinorUnits = 0L,    totalOwedMinorUnits = 500L),
            MemberBalance(3L, "Carol", totalPaidMinorUnits = 0L,    totalOwedMinorUnits = 500L),
        )
        assertEquals(0L, balances.sumOf { it.netMinorUnits })
    }
}
