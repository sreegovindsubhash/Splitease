package com.splitease.domain.usecase

import com.splitease.domain.model.Member
import com.splitease.domain.repository.MemberRepository
import kotlinx.coroutines.flow.Flow

/** Returns a live [Flow] of members belonging to [groupId], ordered by creation time. */
class GetMembersForGroupUseCase(private val repository: MemberRepository) {
    operator fun invoke(groupId: Long): Flow<List<Member>> =
        repository.getMembersForGroup(groupId)
}
