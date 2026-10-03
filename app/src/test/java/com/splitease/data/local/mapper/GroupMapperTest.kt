package com.splitease.data.local.mapper

import com.splitease.data.local.dao.GroupWithStats
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

class GroupWithStatsMappingTest {

    /**
     * Regression: before the SQL fix, the query joined members and expenses without
     * sub-aggregation, causing SUM(expenses.amount) to be multiplied by the number of
     * members.  This test pins the mapper contract: total_amount must flow through
     * unchanged regardless of member_count.
     *
     * Scenario: 3 members, 1 expense = 60 000 minor units (₹600).
     * Expected: memberCount = 3, totalAmountMinorUnits = 60 000 (NOT 180 000).
     */
    @Test
    fun `GroupWithStats toDomain does not multiply totalAmount by memberCount`() {
        val stats = GroupWithStats(
            id = 1L,
            name = "Goa Trip",
            description = "",
            currencyCode = "INR",
            createdAt = 1_000L,
            updatedAt = 2_000L,
            member_count = 3,
            total_amount = 60_000L,
        )

        val group = stats.toDomain()

        assertEquals(3, group.memberCount)
        assertEquals(60_000L, group.totalAmountMinorUnits)
    }

    @Test
    fun `GroupWithStats toDomain with zero members and zero expenses`() {
        val stats = GroupWithStats(
            id = 2L,
            name = "Empty Group",
            description = "",
            currencyCode = "USD",
            createdAt = 0L,
            updatedAt = 0L,
            member_count = 0,
            total_amount = 0L,
        )

        val group = stats.toDomain()

        assertEquals(0, group.memberCount)
        assertEquals(0L, group.totalAmountMinorUnits)
    }

    @Test
    fun `GroupWithStats toDomain preserves all scalar fields`() {
        val stats = GroupWithStats(
            id = 42L,
            name = "Hostel 204",
            description = "Second year room",
            currencyCode = "EUR",
            createdAt = 100L,
            updatedAt = 200L,
            member_count = 5,
            total_amount = 120_000L,
        )

        val group = stats.toDomain()

        assertEquals(42L, group.id)
        assertEquals("Hostel 204", group.name)
        assertEquals("Second year room", group.description)
        assertEquals("EUR", group.currencyCode)
        assertEquals(100L, group.createdAt)
        assertEquals(200L, group.updatedAt)
        assertEquals(5, group.memberCount)
        assertEquals(120_000L, group.totalAmountMinorUnits)
    }
}

class GroupBudgetMapperTest {

    @Test
    fun `GroupEntity toDomain preserves budgetMinorUnits when set`() {
        val entity = GroupEntity(
            id = 1L,
            name = "Trip",
            description = "",
            currencyCode = "INR",
            createdAt = 1L,
            updatedAt = 2L,
            budgetMinorUnits = 50_000L,
        )
        val group = entity.toDomain()
        assertEquals(50_000L, group.budgetMinorUnits)
    }

    @Test
    fun `GroupEntity toDomain with null budget yields null budgetMinorUnits`() {
        val entity = GroupEntity(
            id = 1L,
            name = "Trip",
            description = "",
            currencyCode = "INR",
            createdAt = 1L,
            updatedAt = 2L,
            budgetMinorUnits = null,
        )
        val group = entity.toDomain()
        org.junit.Assert.assertNull(group.budgetMinorUnits)
    }

    @Test
    fun `Group toEntity preserves budgetMinorUnits`() {
        val group = Group(
            id = 1L,
            name = "Trip",
            description = "",
            currencyCode = "INR",
            budgetMinorUnits = 100_00L,
        )
        val entity = group.toEntity()
        assertEquals(100_00L, entity.budgetMinorUnits)
    }

    @Test
    fun `Group toEntity with null budget preserves null`() {
        val group = Group(
            id = 1L,
            name = "Trip",
            description = "",
            currencyCode = "INR",
            budgetMinorUnits = null,
        )
        val entity = group.toEntity()
        org.junit.Assert.assertNull(entity.budgetMinorUnits)
    }

    @Test
    fun `GroupWithStats toDomain preserves budgetMinorUnits`() {
        val stats = GroupWithStats(
            id = 1L,
            name = "Trip",
            description = "",
            currencyCode = "INR",
            createdAt = 1L,
            updatedAt = 2L,
            member_count = 2,
            total_amount = 10_000L,
            budget_minor_units = 50_000L,
        )
        val group = stats.toDomain()
        assertEquals(50_000L, group.budgetMinorUnits)
    }

    @Test
    fun `GroupWithStats toDomain null budget yields null`() {
        val stats = GroupWithStats(
            id = 1L,
            name = "Trip",
            description = "",
            currencyCode = "INR",
            createdAt = 1L,
            updatedAt = 2L,
            member_count = 0,
            total_amount = 0L,
            budget_minor_units = null,
        )
        val group = stats.toDomain()
        org.junit.Assert.assertNull(group.budgetMinorUnits)
    }
}
