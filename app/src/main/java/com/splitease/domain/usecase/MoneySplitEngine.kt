package com.splitease.domain.usecase

/**
 * Pure Kotlin expense-splitting engine.
 *
 * All monetary values are integer minor units (for example, cents/paise).
 * No Double/Float is used for money.
 */
object MoneySplitEngine {

    enum class SplitType { EQUAL, EXACT, PERCENTAGE, SHARES }

    data class Participant(
        val memberId: String,
        val weight: Long = 1L
    )

    data class Allocation(
        val memberId: String,
        val amountMinor: Long
    )

    class SplitException(message: String) : IllegalArgumentException(message)

    /**
     * [percentageBasisPoints] uses 10,000 = 100%.
     * Example: 33.33% = 3333.
     */
    fun split(
        totalMinor: Long,
        type: SplitType,
        participants: List<Participant>,
        exactAmounts: Map<String, Long> = emptyMap(),
        percentageBasisPoints: Map<String, Long> = emptyMap(),
        shares: Map<String, Long> = emptyMap(),
    ): List<Allocation> {
        require(totalMinor >= 0) { "Total amount cannot be negative." }
        validateParticipants(participants)

        return when (type) {
            SplitType.EQUAL -> proportionalSplit(
                totalMinor,
                participants.map { it.memberId to 1L }
            )

            SplitType.EXACT -> exactSplit(
                totalMinor,
                participants.map { it.memberId to (exactAmounts[it.memberId] ?: 0L) }
            )

            SplitType.PERCENTAGE -> {
                val percentages = participants.map {
                    it.memberId to (percentageBasisPoints[it.memberId] ?: 0L)
                }
                require(percentages.all { it.second >= 0L }) {
                    "Percentages cannot be negative."
                }
                require(percentages.sumOf { it.second } == BASIS_POINTS) {
                    "Percentages must total exactly 100% (10000 basis points)."
                }
                proportionalSplit(totalMinor, percentages)
            }

            SplitType.SHARES -> {
                val weights = participants.map {
                    it.memberId to (shares[it.memberId] ?: 0L)
                }
                require(weights.all { it.second > 0L }) {
                    "Each participant must have a positive number of shares."
                }
                proportionalSplit(totalMinor, weights)
            }
        }
    }

    private fun exactSplit(
        totalMinor: Long,
        amounts: List<Pair<String, Long>>
    ): List<Allocation> {
        require(amounts.all { it.second >= 0L }) {
            "Exact amounts cannot be negative."
        }
        require(amounts.sumOf { it.second } == totalMinor) {
            "Exact split amounts must total the expense amount."
        }
        return amounts.map { Allocation(it.first, it.second) }
    }

    /**
     * Integer largest-remainder allocation.
     *
     * Every participant first receives floor(total * weight / totalWeight).
     * Remaining minor units are assigned one at a time to the largest
     * fractional remainders. Input order breaks ties deterministically.
     */
    private fun proportionalSplit(
        totalMinor: Long,
        weights: List<Pair<String, Long>>
    ): List<Allocation> {
        require(weights.isNotEmpty()) { "At least one participant is required." }
        require(weights.all { it.second >= 0L }) {
            "Weights cannot be negative."
        }

        val totalWeight = weights.sumOf { it.second }
        require(totalWeight > 0L) { "Total weight must be greater than zero." }

        val base = weights.map { (id, weight) ->
            val numerator = totalMinor * weight
            val amount = numerator / totalWeight
            val remainder = numerator % totalWeight
            Triple(id, amount, remainder)
        }

        var remaining = totalMinor - base.sumOf { it.second }
        val ranked = base.indices.sortedWith(
            compareByDescending<Int> { base[it].third }
                .thenBy { it }
        )

        val result = base.map { Allocation(it.first, it.second) }.toMutableList()
        var rankIndex = 0
        while (remaining > 0L) {
            val index = ranked[rankIndex % ranked.size]
            result[index] = result[index].copy(amountMinor = result[index].amountMinor + 1L)
            remaining--
            rankIndex++
        }

        check(result.sumOf { it.amountMinor } == totalMinor)
        return result
    }

    private fun validateParticipants(participants: List<Participant>) {
        require(participants.isNotEmpty()) { "At least one participant is required." }
        require(participants.map { it.memberId }.distinct().size == participants.size) {
            "Participant member IDs must be unique."
        }
        require(participants.all { it.memberId.isNotBlank() }) {
            "Participant member IDs cannot be blank."
        }
    }

    private const val BASIS_POINTS = 10_000L
}
