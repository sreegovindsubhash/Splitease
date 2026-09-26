package com.splitease.domain.model

/**
 * A simplified settlement transaction derived from net balances.
 * This is a computed view — not persisted in the database.
 */
data class SettlementTransaction(
    val fromMemberId: Long,
    val fromMemberName: String,
    val toMemberId: Long,
    val toMemberName: String,
    val amountMinorUnits: Long,
)
