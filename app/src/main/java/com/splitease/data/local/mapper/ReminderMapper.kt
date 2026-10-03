package com.splitease.data.local.mapper

import com.splitease.data.local.entity.ReminderEntity
import com.splitease.domain.model.Reminder

fun ReminderEntity.toDomain(): Reminder = Reminder(
    id = id,
    title = title,
    scheduledAt = scheduledAt,
    note = note,
    isCompleted = isCompleted,
)

fun Reminder.toEntity(): ReminderEntity = ReminderEntity(
    id = id,
    title = title,
    scheduledAt = scheduledAt,
    note = note,
    isCompleted = isCompleted,
)
