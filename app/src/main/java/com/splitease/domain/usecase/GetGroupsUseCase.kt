package com.splitease.domain.usecase

import com.splitease.domain.model.Group
import com.splitease.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow

/** Returns a live-updating list of all groups with member count and totals. */
class GetGroupsUseCase(private val repository: GroupRepository) {
    operator fun invoke(): Flow<List<Group>> = repository.getGroups()
}
