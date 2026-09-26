package com.splitease.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MoneySplitEngineTest {

    private val members = listOf(
        MoneySplitEngine.Participant("A"),
        MoneySplitEngine.Participant("B"),
        MoneySplitEngine.Participant("C")
    )

    @Test
    fun equalSplit_reconcilesRemainderDeterministically() {
        val result = MoneySplitEngine.split(
            totalMinor = 100L,
            type = MoneySplitEngine.SplitType.EQUAL,
            participants = members
        )

        assertEquals(
            listOf(
                MoneySplitEngine.Allocation("A", 34),
                MoneySplitEngine.Allocation("B", 33),
                MoneySplitEngine.Allocation("C", 33)
            ),
            result
        )
    }

    @Test
    fun exactSplit_requiresExactReconciliation() {
        val result = MoneySplitEngine.split(
            totalMinor = 1000L,
            type = MoneySplitEngine.SplitType.EXACT,
            participants = members,
            exactAmounts = mapOf("A" to 200L, "B" to 300L, "C" to 500L)
        )

        assertEquals(1000L, result.sumOf { it.amountMinor })
    }

    @Test
    fun exactSplit_rejectsWrongTotal() {
        assertFailsWith<IllegalArgumentException> {
            MoneySplitEngine.split(
                totalMinor = 1000L,
                type = MoneySplitEngine.SplitType.EXACT,
                participants = members,
                exactAmounts = mapOf("A" to 200L, "B" to 300L, "C" to 400L)
            )
        }
    }

    @Test
    fun percentageSplit_usesBasisPoints() {
        val result = MoneySplitEngine.split(
            totalMinor = 10001L,
            type = MoneySplitEngine.SplitType.PERCENTAGE,
            participants = members,
            percentageBasisPoints = mapOf(
                "A" to 3333L,
                "B" to 3333L,
                "C" to 3334L
            )
        )

        assertEquals(10001L, result.sumOf { it.amountMinor })
        assertEquals(listOf(3333L, 3333L, 3335L), result.map { it.amountMinor })    }

    @Test
    fun sharesSplit_reconcilesExactly() {
        val result = MoneySplitEngine.split(
            totalMinor = 100L,
            type = MoneySplitEngine.SplitType.SHARES,
            participants = members,
            shares = mapOf("A" to 1L, "B" to 2L, "C" to 3L)
        )

        assertEquals(100L, result.sumOf { it.amountMinor })
        assertEquals(listOf(17L, 33L, 50L), result.map { it.amountMinor })
    }

    @Test
    fun sharesSplit_rejectsZeroShares() {
        assertFailsWith<IllegalArgumentException> {
            MoneySplitEngine.split(
                totalMinor = 100L,
                type = MoneySplitEngine.SplitType.SHARES,
                participants = members,
                shares = mapOf("A" to 1L, "B" to 0L, "C" to 1L)
            )
        }
    }
}
