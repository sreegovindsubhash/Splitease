package com.splitease.domain.usecase

import com.splitease.domain.model.Group
import com.splitease.domain.repository.GroupRepository

/**
 * Returns the [Group] with the given [id], or null if it does not exist.
 * Delegates entirely to the repository; never touches Room directly.
 */
class GetGroupByIdUseCase(private val repository: GroupRepository) {
    suspend operator fun invoke(id: Long): Group? = repository.getGroupById(id)
}
