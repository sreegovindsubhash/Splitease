package com.splitease.data.local.mapper

import com.splitease.data.local.entity.MemberEntity
import com.splitease.domain.model.Member
import org.junit.Assert.assertEquals
import org.junit.Test

class MemberMapperTest {

    @Test
    fun `MemberEntity toDomain preserves all fields`() {
        val entity = MemberEntity(
            id = 5L,
            groupId = 2L,
            name = "Alice",
            email = "alice@example.com",
            createdAt = 500L,
        )
        val domain = entity.toDomain()
        assertEquals(5L, domain.id)
        assertEquals(2L, domain.groupId)
        assertEquals("Alice", domain.name)
        assertEquals("alice@example.com", domain.email)
        assertEquals(500L, domain.createdAt)
    }

    @Test
    fun `Member toEntity round-trip preserves all fields`() {
        val member = Member(
            id = 9L,
            groupId = 3L,
            name = "Bob",
            email = "",
            createdAt = 999L,
        )
        val entity = member.toEntity()
        assertEquals(9L, entity.id)
        assertEquals(3L, entity.groupId)
        assertEquals("Bob", entity.name)
        assertEquals("", entity.email)
        assertEquals(999L, entity.createdAt)
    }

    @Test
    fun `MemberEntity toEntity toDomain is a round-trip`() {
        val original = MemberEntity(
            id = 1L,
            groupId = 1L,
            name = "Charlie",
            email = "c@c.com",
            createdAt = 1L,
        )
        assertEquals(original, original.toDomain().toEntity())
    }
}
