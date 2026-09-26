package com.splitease.domain.usecase

import com.splitease.domain.model.Group
import com.splitease.domain.repository.GroupRepository

/**
 * Updates an existing group's name and description.
 * Currency is intentionally excluded — existing expenses are denominated in the group's
 * currency, and changing it would create ambiguous financial data.
 */
class UpdateGroupUseCase(private val repository: GroupRepository) {

    suspend operator fun invoke(
        existing: Group,
        newName: String,
        newDescription: String,
    ): Group {
        val trimmedName = newName.trim()
        require(trimmedName.isNotBlank()) { "Group name must not be blank" }

        val updated = existing.copy(
            name = trimmedName,
            description = newDescription.trim(),
            updatedAt = System.currentTimeMillis(),
        )
        repository.updateGroup(updated)
        return updated
    }
}
