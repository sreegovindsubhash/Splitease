package com.splitease.domain.model

/**
 * Persisted record that a debtor has paid a creditor some amount.
 *
 * This is a user-entered record (not a financial transaction from expenses).
 * It reduces the outstanding settlement obligation between the two members.
 *
 * All monetary values are Long minor units (paise, cents, etc.) — never Double/Float.
 */
data class SettlementPayment(
    val id: Long = 0,
    val groupId: Long,
    val debtorMemberId: Long,
    val creditorMemberId: Long,
    /** Amount paid, always > 0. Long minor units. */
    val amountMinorUnits: Long,
    /** Unix epoch milliseconds when the payment was recorded. */
    val paidAt: Long = System.currentTimeMillis(),
)
