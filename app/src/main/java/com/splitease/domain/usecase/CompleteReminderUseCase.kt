package com.splitease.domain.usecase

import com.splitease.domain.model.Reminder
import com.splitease.domain.repository.ReminderRepository

/** Marks a reminder as completed. */
class CompleteReminderUseCase(private val repository: ReminderRepository) {
    suspend operator fun invoke(reminder: Reminder) {
        repository.update(reminder.copy(isCompleted = true))
    }
}
