package com.splitease.domain.repository

import com.splitease.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun getAll(): Flow<List<Reminder>>
    suspend fun getActiveFutureReminders(nowMs: Long): List<Reminder>
    suspend fun getById(id: Long): Reminder?
    suspend fun add(reminder: Reminder): Long
    suspend fun update(reminder: Reminder)
    suspend fun delete(reminder: Reminder)
}
