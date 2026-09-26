package com.splitease.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DebtSimplifierTest {

    @Test
    fun simplifiesToDeterministicTransfers() {
        val result = DebtSimplifier.simplify(
            listOf(
                DebtSimplifier.Balance(1L, -700L),
                DebtSimplifier.Balance(2L, 300L),
                DebtSimplifier.Balance(3L, 400L)
            )
        )

        assertEquals(
            listOf(
                DebtSimplifier.Transfer(1L, 2L, 300L),
                DebtSimplifier.Transfer(1L, 3L, 400L)
            ),
            result
        )
    }

    @Test
    fun balancesMustSumToZero() {
        assertFailsWith<IllegalArgumentException> {
            DebtSimplifier.simplify(
                listOf(
                    DebtSimplifier.Balance(1L, -100L),
                    DebtSimplifier.Balance(2L, 50L)
                )
            )
        }
    }

    @Test
    fun zeroBalancesProduceNoTransfers() {
        assertEquals(
            emptyList(),
            DebtSimplifier.simplify(
                listOf(
                    DebtSimplifier.Balance(1L, 0L),
                    DebtSimplifier.Balance(2L, 0L)
                )
            )
        )
    }
}
