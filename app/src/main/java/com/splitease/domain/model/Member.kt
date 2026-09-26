package com.splitease.domain.model

/** Domain model for a Member belonging to a Group. */
data class Member(
    val id: Long = 0,
    val groupId: Long,
    val name: String,
    val email: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
