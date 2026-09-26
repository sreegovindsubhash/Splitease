package com.splitease.domain.usecase

import com.splitease.domain.model.Member
import com.splitease.domain.repository.MemberRepository

/** Validates and updates an existing [Member]. */
class UpdateMemberUseCase(private val repository: MemberRepository) {

    suspend operator fun invoke(member: Member, newName: String) {
        val trimmed = newName.trim()
        require(trimmed.isNotBlank()) { "Member name is required" }
        repository.updateMember(member.copy(name = trimmed))
    }
}
