package com.splitease.data.repository

import com.splitease.data.local.dao.SettlementPaymentDao
import com.splitease.data.local.mapper.toDomain
import com.splitease.data.local.mapper.toEntity
import com.splitease.domain.model.SettlementPayment
import com.splitease.domain.repository.SettlementPaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettlementPaymentRepositoryImpl(
    private val dao: SettlementPaymentDao,
) : SettlementPaymentRepository {

    override fun getPaymentsForGroup(groupId: Long): Flow<List<SettlementPayment>> =
        dao.getPaymentsForGroup(groupId).map { list -> list.map { it.toDomain() } }

    override suspend fun recordPayment(payment: SettlementPayment): Long =
        dao.insertPayment(payment.toEntity())

    override suspend fun deletePayment(paymentId: Long) =
        dao.deletePaymentById(paymentId)
}
