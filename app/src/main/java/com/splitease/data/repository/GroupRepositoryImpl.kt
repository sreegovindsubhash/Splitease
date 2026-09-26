package com.splitease.data.repository

import com.splitease.data.local.dao.GroupDao
import com.splitease.data.local.dao.MemberDao
import com.splitease.data.local.mapper.toDomain
import com.splitease.data.local.mapper.toEntity
import com.splitease.domain.model.Group
import com.splitease.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GroupRepositoryImpl(
    private val groupDao: GroupDao,
    private val memberDao: MemberDao,
) : GroupRepository {

    override fun getGroups(): Flow<List<Group>> =
        groupDao.getGroupsWithStats().map { list -> list.map { it.toDomain() } }

    override fun observeGroupById(id: Long): Flow<Group?> =
        groupDao.getGroupWithStatsById(id).map { it?.toDomain() }

    override suspend fun getGroupById(id: Long): Group? =
        groupDao.getGroupById(id)?.toDomain()

    override suspend fun createGroup(group: Group): Long =
        groupDao.insertGroup(group.toEntity())

    override suspend fun updateGroup(group: Group) =
        groupDao.updateGroup(group.toEntity())

    override suspend fun deleteGroup(group: Group) =
        groupDao.deleteGroup(group.toEntity())
}
