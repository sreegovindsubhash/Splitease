package com.splitease.domain.repository

import com.splitease.domain.model.Member
import kotlinx.coroutines.flow.Flow

interface MemberRepository {
    fun getMembersForGroup(groupId: Long): Flow<List<Member>>
    suspend fun getMemberById(id: Long): Member?
    suspend fun addMember(member: Member): Long
    suspend fun updateMember(member: Member)
    suspend fun deleteMember(member: Member)
}
