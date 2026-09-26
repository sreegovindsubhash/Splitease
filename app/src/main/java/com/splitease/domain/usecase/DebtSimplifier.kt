package com.splitease.domain.usecase

/**
 * Converts member net balances into deterministic settlement transfers.
 *
 * Positive balance = member should receive money.
 * Negative balance = member owes money.
 *
 * This uses a deterministic two-pointer settlement algorithm. It produces
 * at most (number of non-zero balances - 1) transfers and never creates
 * self-transfers. It is intentionally pure Kotlin and independent of Room/UI.
 */
object DebtSimplifier {

    data class Balance(
        val memberId: Long,
        val amountMinor: Long
    )

    data class Transfer(
        val fromMemberId: Long,
        val toMemberId: Long,
        val amountMinor: Long
    )

    fun simplify(balances: List<Balance>): List<Transfer> {
        require(balances.map { it.memberId }.distinct().size == balances.size) {
            "Member IDs must be unique."
        }

        val total = balances.sumOf { it.amountMinor }
        require(total == 0L) {
            "Balances must sum to zero."
        }

        val debtors = balances
            .filter { it.amountMinor < 0L }
            .map { it.memberId to -it.amountMinor }
            .sortedBy { it.first }
            .toMutableList()

        val creditors = balances
            .filter { it.amountMinor > 0L }
            .map { it.memberId to it.amountMinor }
            .sortedBy { it.first }
            .toMutableList()

        val result = mutableListOf<Transfer>()
        var debtorIndex = 0
        var creditorIndex = 0

        while (debtorIndex < debtors.size && creditorIndex < creditors.size) {
            val (debtorId, debt) = debtors[debtorIndex]
            val (creditorId, credit) = creditors[creditorIndex]
            val amount = minOf(debt, credit)

            if (amount > 0L) {
                result += Transfer(
                    fromMemberId = debtorId,
                    toMemberId = creditorId,
                    amountMinor = amount
                )
            }

            val remainingDebt = debt - amount
            val remainingCredit = credit - amount

            if (remainingDebt == 0L) debtorIndex++
            else debtors[debtorIndex] = debtorId to remainingDebt

            if (remainingCredit == 0L) creditorIndex++
            else creditors[creditorIndex] = creditorId to remainingCredit
        }

        return result
    }
}
