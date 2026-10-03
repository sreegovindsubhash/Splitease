package com.splitease.domain.usecase

import com.splitease.domain.model.Reminder
import com.splitease.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow

/** Returns a live [Flow] of all reminders, ordered by scheduled time ascending. */
class GetRemindersUseCase(private val repository: ReminderRepository) {
    operator fun invoke(): Flow<List<Reminder>> = repository.getAll()
}
