package com.splitease.domain.model

/**
 * Represents a single participant's share in an expense.
 * [shareMinorUnits] must be exactly allocated so that sum(shares) == expense.amountMinorUnits.
 */
data class ExpenseSplit(
    val id: Long = 0,
    val expenseId: Long,
    val memberId: Long,
    val shareMinorUnits: Long,
)
