package com.splitease.data.local.mapper

import com.splitease.data.local.entity.SettlementPaymentEntity
import com.splitease.domain.model.SettlementPayment

fun SettlementPaymentEntity.toDomain(): SettlementPayment = SettlementPayment(
    id = id,
    groupId = groupId,
    debtorMemberId = debtorMemberId,
    creditorMemberId = creditorMemberId,
    amountMinorUnits = amountMinorUnits,
    paidAt = paidAt,
)

fun SettlementPayment.toEntity(): SettlementPaymentEntity = SettlementPaymentEntity(
    id = id,
    groupId = groupId,
    debtorMemberId = debtorMemberId,
    creditorMemberId = creditorMemberId,
    amountMinorUnits = amountMinorUnits,
    paidAt = paidAt,
)
