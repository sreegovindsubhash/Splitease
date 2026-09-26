package com.splitease.data.local.mapper

import com.splitease.data.local.entity.GroupEntity
import com.splitease.domain.model.Group
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupMapperTest {

    @Test
    fun `GroupEntity toDomain preserves all fields`() {
        val entity = GroupEntity(
            id = 42L,
            name = "Goa Trip",
            description = "Beach vacation",
            currencyCode = "INR",
            createdAt = 1_000L,
            updatedAt = 2_000L,
        )
        val domain = entity.toDomain()
        assertEquals(42L, domain.id)
        assertEquals("Goa Trip", domain.name)
        assertEquals("Beach vacation", domain.description)
        assertEquals("INR", domain.currencyCode)
        assertEquals(1_000L, domain.createdAt)
        assertEquals(2_000L, domain.updatedAt)
    }

    @Test
    fun `Group toEntity round-trip preserves all fields`() {
        val group = Group(
            id = 7L,
            name = "Hostel 204",
            description = "",
            currencyCode = "USD",
            createdAt = 100L,
            updatedAt = 200L,
        )
        val entity = group.toEntity()
        assertEquals(7L, entity.id)
        assertEquals("Hostel 204", entity.name)
        assertEquals("", entity.description)
        assertEquals("USD", entity.currencyCode)
        assertEquals(100L, entity.createdAt)
        assertEquals(200L, entity.updatedAt)
    }

    @Test
    fun `GroupEntity toEntity toDomain is a round-trip`() {
        val original = GroupEntity(
            id = 1L,
            name = "Trip",
            description = "Note",
            currencyCode = "EUR",
            createdAt = 10L,
            updatedAt = 20L,
        )
        val roundTripped = original.toDomain().toEntity()
        assertEquals(original, roundTripped)
    }
}
