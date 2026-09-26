package com.splitease.data.repository

import com.splitease.data.local.dao.MemberDao
import com.splitease.data.local.mapper.toDomain
import com.splitease.data.local.mapper.toEntity
import com.splitease.domain.model.Member
import com.splitease.domain.repository.MemberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MemberRepositoryImpl(
    private val memberDao: MemberDao,
) : MemberRepository {

    override fun getMembersForGroup(groupId: Long): Flow<List<Member>> =
        memberDao.getMembersForGroup(groupId).map { list -> list.map { it.toDomain() } }

    override suspend fun getMemberById(id: Long): Member? =
        memberDao.getMemberById(id)?.toDomain()

    override suspend fun addMember(member: Member): Long =
        memberDao.insertMember(member.toEntity())

    override suspend fun updateMember(member: Member) =
        memberDao.updateMember(member.toEntity())

    override suspend fun deleteMember(member: Member) =
        memberDao.deleteMember(member.toEntity())
}
