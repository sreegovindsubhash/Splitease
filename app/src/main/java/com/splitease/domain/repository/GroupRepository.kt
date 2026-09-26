package com.splitease.domain.repository

import com.splitease.domain.model.Group
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    fun getGroups(): Flow<List<Group>>
    suspend fun getGroupById(id: Long): Group?
    suspend fun createGroup(group: Group): Long
    suspend fun updateGroup(group: Group)
    suspend fun deleteGroup(group: Group)
}
