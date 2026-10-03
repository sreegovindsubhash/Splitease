package com.splitease.domain.usecase

import com.splitease.domain.model.Group
import com.splitease.domain.repository.GroupRepository

/**
 * Sets or replaces the optional budget for a group.
 *
 * @param budgetMinorUnits must be > 0. Pass null to clear the budget (not exposed in UI,
 *   but useful for testing / future use).
 * @throws IllegalArgumentException if [budgetMinorUnits] is provided but not positive.
 */
class SetGroupBudgetUseCase(private val repository: GroupRepository) {

    suspend operator fun invoke(group: Group, budgetMinorUnits: Long?): Group {
        if (budgetMinorUnits != null) {
            require(budgetMinorUnits > 0L) { "Budget must be greater than zero" }
        }
        val updated = group.copy(
            budgetMinorUnits = budgetMinorUnits,
            updatedAt = System.currentTimeMillis(),
        )
        repository.updateGroup(updated)
        return updated
    }
}
