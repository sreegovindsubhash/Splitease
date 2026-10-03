package com.splitease.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.splitease.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    /** Emits all reminders ordered by scheduled time. */
    @Query("SELECT * FROM reminders ORDER BY scheduled_at ASC")
    fun getAll(): Flow<List<ReminderEntity>>

    /** Returns all active (not completed) reminders whose time is in the future. */
    @Query("SELECT * FROM reminders WHERE is_completed = 0 AND scheduled_at > :nowMs ORDER BY scheduled_at ASC")
    suspend fun getActiveFutureReminders(nowMs: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)
}
