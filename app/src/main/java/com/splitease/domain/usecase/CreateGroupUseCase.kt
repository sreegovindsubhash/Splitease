package com.splitease.domain.usecase

import com.splitease.domain.model.Group
import com.splitease.domain.repository.GroupRepository

/** Creates a new group and returns its generated ID. */
class CreateGroupUseCase(private val repository: GroupRepository) {
    suspend operator fun invoke(name: String, currencyCode: String, description: String): Long {
        require(name.isNotBlank()) { "Group name must not be blank" }
        val group = Group(
            name = name.trim(),
            description = description.trim(),
            currencyCode = currencyCode,
        )
        return repository.createGroup(group)
    }
}
