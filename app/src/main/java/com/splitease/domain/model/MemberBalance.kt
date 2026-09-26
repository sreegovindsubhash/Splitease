package com.splitease.domain.model

/**
 * Per-member balance summary for a group.
 * Positive [netMinorUnits] means the member is owed money.
 * Negative means the member owes money.
 */
data class MemberBalance(
    val memberId: Long,
    val memberName: String,
    val totalPaidMinorUnits: Long,
    val totalOwedMinorUnits: Long,
    val netMinorUnits: Long = totalPaidMinorUnits - totalOwedMinorUnits,
)
