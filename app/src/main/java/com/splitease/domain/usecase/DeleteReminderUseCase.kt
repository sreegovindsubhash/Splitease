package com.splitease.domain.usecase

import com.splitease.domain.model.Reminder
import com.splitease.domain.repository.ReminderRepository

/** Deletes a reminder. */
class DeleteReminderUseCase(private val repository: ReminderRepository) {
    suspend operator fun invoke(reminder: Reminder) = repository.delete(reminder)
}
