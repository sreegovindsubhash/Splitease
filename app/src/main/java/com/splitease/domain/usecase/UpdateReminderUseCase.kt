package com.splitease.domain.usecase

import com.splitease.domain.model.Reminder
import com.splitease.domain.repository.ReminderRepository

/**
 * Updates an existing reminder.
 *
 * Validation:
 * - [Reminder.id] must be > 0 (must be a persisted record).
 * - [Reminder.title] must not be blank.
 * - [Reminder.scheduledAt] must be a positive epoch value.
 */
class UpdateReminderUseCase(private val repository: ReminderRepository) {

    suspend operator fun invoke(reminder: Reminder) {
        require(reminder.id > 0) { "Cannot update a reminder that has not been saved (id = 0)." }
        require(reminder.title.isNotBlank()) { "Reminder title must not be blank." }
        require(reminder.scheduledAt > 0) { "Reminder scheduledAt must be a valid epoch time." }
        repository.update(reminder)
    }
}
