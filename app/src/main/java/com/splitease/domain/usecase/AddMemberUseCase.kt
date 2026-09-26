package com.splitease.domain.usecase

import com.splitease.domain.model.Member
import com.splitease.domain.repository.MemberRepository

/** Validates and persists a new [Member] to the repository. Returns the new member's id. */
class AddMemberUseCase(private val repository: MemberRepository) {

    suspend operator fun invoke(groupId: Long, name: String): Long {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "Member name is required" }
        return repository.addMember(
            Member(groupId = groupId, name = trimmed),
        )
    }
}
