package com.splitease.domain.usecase

import com.splitease.domain.model.Group
import com.splitease.domain.repository.GroupRepository

/**
 * Deletes a group and all its dependent data.
 *
 * Data cleanup relies on Room/SQLite CASCADE foreign keys:
 *   - members          → groups(id)  CASCADE
 *   - expenses         → groups(id)  CASCADE
 *   - expense_splits   → expenses(id) CASCADE  (removed when expenses go)
 *   - settlement_payments → groups(id) CASCADE
 *
 * SQLite processes cascades depth-first, so expenses and settlement_payments
 * are removed before members, satisfying the RESTRICT constraints on member FKs.
 */
class DeleteGroupUseCase(private val repository: GroupRepository) {

    suspend operator fun invoke(group: Group) {
        repository.deleteGroup(group)
    }
}
