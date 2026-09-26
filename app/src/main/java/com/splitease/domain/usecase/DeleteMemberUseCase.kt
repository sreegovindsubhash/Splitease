package com.splitease.domain.usecase

import com.splitease.domain.model.Member
import com.splitease.domain.repository.MemberRepository

/** Removes a [Member] from the repository. */
class DeleteMemberUseCase(private val repository: MemberRepository) {

    suspend operator fun invoke(member: Member) {
        repository.deleteMember(member)
    }
}
