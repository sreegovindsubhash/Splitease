package com.splitease.data.repository

import com.splitease.data.local.dao.ReminderDao
import com.splitease.data.local.mapper.toDomain
import com.splitease.data.local.mapper.toEntity
import com.splitease.domain.model.Reminder
import com.splitease.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ReminderRepositoryImpl(
    private val reminderDao: ReminderDao,
) : ReminderRepository {

    override fun getAll(): Flow<List<Reminder>> =
        reminderDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getActiveFutureReminders(nowMs: Long): List<Reminder> =
        reminderDao.getActiveFutureReminders(nowMs).map { it.toDomain() }

    override suspend fun getById(id: Long): Reminder? =
        reminderDao.getById(id)?.toDomain()

    override suspend fun add(reminder: Reminder): Long =
        reminderDao.insert(reminder.toEntity())

    override suspend fun update(reminder: Reminder) =
        reminderDao.update(reminder.toEntity())

    override suspend fun delete(reminder: Reminder) =
        reminderDao.delete(reminder.toEntity())
}
