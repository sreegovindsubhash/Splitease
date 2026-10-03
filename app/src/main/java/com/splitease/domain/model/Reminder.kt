package com.splitease.domain.model

/**
 * Domain model for a simple one-time expense reminder.
 *
 * [scheduledAt] is epoch milliseconds (UTC).
 * Financial amounts are intentionally absent — this is a lightweight reminder only.
 */
data class Reminder(
    val id: Long = 0,
    val title: String,
    val scheduledAt: Long,
    val note: String = "",
    val isCompleted: Boolean = false,
)
