package com.splitease.domain.usecase

import com.splitease.domain.model.Reminder
import com.splitease.domain.repository.ReminderRepository

/**
 * Adds a new reminder.
 *
 * Validation:
 * - [Reminder.title] must not be blank.
 * - [Reminder.scheduledAt] must be a positive epoch value.
 *
 * @return the auto-generated ID of the inserted reminder.
 */
class AddReminderUseCase(private val repository: ReminderRepository) {

    suspend operator fun invoke(reminder: Reminder): Long {
        require(reminder.title.isNotBlank()) { "Reminder title must not be blank." }
        require(reminder.scheduledAt > 0) { "Reminder scheduledAt must be a valid epoch time." }
        return repository.add(reminder)
    }
}
