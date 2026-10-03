package com.splitease.data.local.mapper

import com.splitease.data.local.entity.ReminderEntity
import com.splitease.domain.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderMapperTest {

    private val FAR_FUTURE = 1_800_000_000_000L

    @Test
    fun `ReminderEntity toDomain preserves all fields`() {
        val entity = ReminderEntity(
            id = 7L,
            title = "Pay electricity",
            scheduledAt = FAR_FUTURE,
            note = "Remember the late fee",
            isCompleted = false,
        )
        val domain = entity.toDomain()
        assertEquals(7L, domain.id)
        assertEquals("Pay electricity", domain.title)
        assertEquals(FAR_FUTURE, domain.scheduledAt)
        assertEquals("Remember the late fee", domain.note)
        assertEquals(false, domain.isCompleted)
    }

    @Test
    fun `Reminder toEntity round-trip preserves all fields`() {
        val entity = ReminderEntity(
            id = 3L,
            title = "Groceries",
            scheduledAt = FAR_FUTURE,
            note = "",
            isCompleted = true,
        )
        assertEquals(entity, entity.toDomain().toEntity())
    }

    @Test
    fun `ReminderEntity toDomain with empty note`() {
        val entity = ReminderEntity(id = 1L, title = "Test", scheduledAt = FAR_FUTURE)
        val domain = entity.toDomain()
        assertEquals("", domain.note)
        assertEquals(false, domain.isCompleted)
    }

    @Test
    fun `scheduledAt is preserved exactly as Long`() {
        val exactMs = 1_700_000_000_123L  // non-round value
        val entity = ReminderEntity(id = 1L, title = "T", scheduledAt = exactMs)
        assertEquals(exactMs, entity.toDomain().scheduledAt)
        assertEquals(exactMs, entity.toDomain().toEntity().scheduledAt)
    }
}
