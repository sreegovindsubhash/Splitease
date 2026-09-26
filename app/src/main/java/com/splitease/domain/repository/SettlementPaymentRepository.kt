package com.splitease.domain.repository

import com.splitease.domain.model.SettlementPayment
import kotlinx.coroutines.flow.Flow

interface SettlementPaymentRepository {
    /** Observe all recorded payments for a group, ordered newest first. */
    fun getPaymentsForGroup(groupId: Long): Flow<List<SettlementPayment>>

    /** Record that a debtor paid a creditor. Returns the generated payment id. */
    suspend fun recordPayment(payment: SettlementPayment): Long

    /** Remove a previously recorded payment (undo/reverse). */
    suspend fun deletePayment(paymentId: Long)
}
