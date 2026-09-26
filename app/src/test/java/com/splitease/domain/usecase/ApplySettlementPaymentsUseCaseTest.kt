package com.splitease.domain.usecase

import com.splitease.domain.model.SettlementPayment
import com.splitease.domain.model.SettlementTransaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [ApplySettlementPaymentsUseCase].
 *
 * All monetary assertions use Long minor units only — no Double/Float.
 */
class ApplySettlementPaymentsUseCaseTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun txn(from: Long, to: Long, amount: Long) = SettlementTransaction(
        fromMemberId = from,
        fromMemberName = "Member$from",
        toMemberId = to,
        toMemberName = "Member$to",
        amountMinorUnits = amount,
    )

    private fun payment(debtor: Long, creditor: Long, amount: Long) = SettlementPayment(
        id = 0L,
        groupId = 1L,
        debtorMemberId = debtor,
        creditorMemberId = creditor,
        amountMinorUnits = amount,
        paidAt = 1_700_000_000_000L,
    )

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun noPayments_returnsSuggestedUnchanged() {
        val suggested = listOf(txn(1L, 2L, 50_000L), txn(3L, 2L, 30_000L))
        val result = ApplySettlementPaymentsUseCase(suggested, emptyList())
        assertEquals(suggested, result)
    }

    @Test
    fun fullPayment_removesTransaction() {
        val suggested = listOf(txn(1L, 2L, 50_000L))
        val result = ApplySettlementPaymentsUseCase(suggested, listOf(payment(1L, 2L, 50_000L)))
        assertTrue(result.isEmpty(), "Full payment should remove the outstanding transaction")
    }

    @Test
    fun partialPayment_reducesAmount() {
        val suggested = listOf(txn(1L, 2L, 20_000L))
        val result = ApplySettlementPaymentsUseCase(suggested, listOf(payment(1L, 2L, 10_000L)))
        assertEquals(1, result.size)
        assertEquals(10_000L, result.single().amountMinorUnits)
    }

    @Test
    fun multiplePartialPayments_reduceCumulatively() {
        // Total paid: 7000, outstanding: 10000 → remaining: 3000
        val suggested = listOf(txn(1L, 2L, 10_000L))
        val payments = listOf(
            payment(1L, 2L, 3_000L),
            payment(1L, 2L, 4_000L),
        )
        val result = ApplySettlementPaymentsUseCase(suggested, payments)
        assertEquals(1, result.size)
        assertEquals(3_000L, result.single().amountMinorUnits)
    }

    @Test
    fun exactCumulativePayments_removeTransaction() {
        val suggested = listOf(txn(1L, 2L, 10_000L))
        val payments = listOf(
            payment(1L, 2L, 6_000L),
            payment(1L, 2L, 4_000L),
        )
        val result = ApplySettlementPaymentsUseCase(suggested, payments)
        assertTrue(result.isEmpty(), "Cumulative payments equalling outstanding should remove transaction")
    }

    @Test
    fun unrelatedPayment_doesNotAffectOtherTransaction() {
        val suggested = listOf(txn(1L, 2L, 50_000L), txn(3L, 4L, 30_000L))
        // Payment is for 3→4, must not affect 1→2
        val result = ApplySettlementPaymentsUseCase(suggested, listOf(payment(3L, 4L, 30_000L)))
        assertEquals(1, result.size)
        assertEquals(50_000L, result.single().amountMinorUnits)
        assertEquals(1L, result.single().fromMemberId)
    }

    @Test
    fun allDebtsFullyPaid_returnsEmpty() {
        val suggested = listOf(
            txn(1L, 3L, 10_000L),
            txn(2L, 3L, 10_000L),
        )
        val payments = listOf(
            payment(1L, 3L, 10_000L),
            payment(2L, 3L, 10_000L),
        )
        val result = ApplySettlementPaymentsUseCase(suggested, payments)
        assertTrue(result.isEmpty(), "All debts paid should produce empty result")
    }

    @Test
    fun paymentExceedsOutstanding_transactionRemovedNotNegative() {
        // Over-payment clamps to zero — no negative amounts
        val suggested = listOf(txn(1L, 2L, 5_000L))
        val result = ApplySettlementPaymentsUseCase(suggested, listOf(payment(1L, 2L, 10_000L)))
        assertTrue(result.isEmpty(), "Over-payment should remove the transaction (clamped to 0)")
    }

    @Test
    fun multipleDebtorsMultipleCreditors() {
        val suggested = listOf(
            txn(1L, 3L, 1_000L),
            txn(2L, 3L, 1_000L),
            txn(1L, 4L, 2_000L),
        )
        val payments = listOf(
            payment(1L, 3L, 500L),   // reduces 1→3 to 500
            payment(2L, 3L, 1_000L), // fully settles 2→3
        )
        val result = ApplySettlementPaymentsUseCase(suggested, payments)
        assertEquals(2, result.size)
        val txn1to3 = result.find { it.fromMemberId == 1L && it.toMemberId == 3L }
        val txn1to4 = result.find { it.fromMemberId == 1L && it.toMemberId == 4L }
        assertEquals(500L, txn1to3?.amountMinorUnits)
        assertEquals(2_000L, txn1to4?.amountMinorUnits)
        assertTrue(result.none { it.fromMemberId == 2L }, "2→3 should be fully settled")
    }

    @Test
    fun exactLongMinorUnitAmounts_noPrecisionLoss() {
        // 1 paisa precision check
        val suggested = listOf(txn(1L, 2L, 100_001L))
        val result = ApplySettlementPaymentsUseCase(suggested, listOf(payment(1L, 2L, 1L)))
        assertEquals(100_000L, result.single().amountMinorUnits)
    }

    @Test
    fun emptyInput_returnsEmpty() {
        val result = ApplySettlementPaymentsUseCase(emptyList(), emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun deterministicOutput_sameInputSameOutput() {
        val suggested = listOf(txn(1L, 3L, 5_000L), txn(2L, 3L, 3_000L))
        val payments = listOf(payment(1L, 3L, 2_000L))
        val run1 = ApplySettlementPaymentsUseCase(suggested, payments)
        val run2 = ApplySettlementPaymentsUseCase(suggested, payments)
        assertEquals(run1, run2, "Result must be deterministic")
    }

    @Test
    fun zeroBalanceTransactionsNotCreated() {
        // Ensure no zero-amount transactions appear in the result
        val suggested = listOf(txn(1L, 2L, 5_000L))
        val result = ApplySettlementPaymentsUseCase(suggested, listOf(payment(1L, 2L, 5_000L)))
        assertTrue(result.all { it.amountMinorUnits > 0L })
    }
}
