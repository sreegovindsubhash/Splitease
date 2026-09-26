package com.splitease.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.splitease.data.local.entity.SettlementPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettlementPaymentDao {

    /** Observe all recorded payments for a group, ordered newest first. */
    @Query(
        "SELECT * FROM settlement_payments WHERE group_id = :groupId ORDER BY paid_at DESC"
    )
    fun getPaymentsForGroup(groupId: Long): Flow<List<SettlementPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPayment(payment: SettlementPaymentEntity): Long

    /** Reverse / delete a single payment record by its id. */
    @Query("DELETE FROM settlement_payments WHERE id = :paymentId")
    suspend fun deletePaymentById(paymentId: Long)
}
