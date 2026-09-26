package com.splitease.domain.usecase

import com.splitease.domain.model.SettlementPayment
import com.splitease.domain.model.SettlementTransaction

/**
 * Reduces the outstanding [SettlementTransaction] list by subtracting recorded
 * [SettlementPayment]s.
 *
 * Rules:
 *   - Only payments whose (debtorMemberId, creditorMemberId) pair matches a transaction
 *     have any effect.
 *   - Payments reduce the outstanding amount; if the amount reaches zero the transaction
 *     is removed from the result.
 *   - Amounts never become negative (over-payment is silently clamped to zero, removing
 *     the transaction). In practice the ViewModel validates payment amounts before recording.
 *   - Unrelated payments are ignored for the purpose of outstanding suggestions.
 *   - All arithmetic is Long only — no Double/Float.
 *
 * This is a pure function — no side effects, no Room access.
 */
object ApplySettlementPaymentsUseCase {

    operator fun invoke(
        suggested: List<SettlementTransaction>,
        payments: List<SettlementPayment>,
    ): List<SettlementTransaction> {
        if (payments.isEmpty()) return suggested

        // Accumulate total paid per (debtor, creditor) pair.
        val paidMap = mutableMapOf<Pair<Long, Long>, Long>()
        for (p in payments) {
            val key = p.debtorMemberId to p.creditorMemberId
            paidMap[key] = (paidMap[key] ?: 0L) + p.amountMinorUnits
        }

        val result = mutableListOf<SettlementTransaction>()
        for (txn in suggested) {
            val key = txn.fromMemberId to txn.toMemberId
            val paid = paidMap[key] ?: 0L
            val remaining = maxOf(0L, txn.amountMinorUnits - paid)
            if (remaining > 0L) {
                result += txn.copy(amountMinorUnits = remaining)
            }
            // remaining == 0 → transaction fully satisfied, omit from result
        }
        return result
    }
}
