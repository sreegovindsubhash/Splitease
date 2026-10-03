package com.splitease.domain.model

/**
 * Domain model for a Group.
 * [totalAmountMinorUnits] is stored as Long minor units (e.g. paise, cents).
 */
data class Group(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val currencyCode: String = "INR",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val memberCount: Int = 0,
    val totalAmountMinorUnits: Long = 0L,
    /** Optional group-level budget stored as Long minor units. Null = no budget set. */
    val budgetMinorUnits: Long? = null,
)
