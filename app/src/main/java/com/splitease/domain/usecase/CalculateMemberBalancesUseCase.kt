package com.splitease.domain.usecase

import com.splitease.domain.model.Expense
import com.splitease.domain.model.ExpenseSplit
import com.splitease.domain.model.MemberBalance

/**
 * Calculates each member's net balance for a group.
 *
 * Positive netMinorUnits  -> member should receive money.
 * Negative netMinorUnits  -> member owes money.
 *
 * All monetary values are Long minor units.
 */
class CalculateMemberBalancesUseCase {

    operator fun invoke(
        groupId: Long,
        expenses: List<Expense>,
        splits: List<ExpenseSplit>,
        memberNames: Map<Long, String>
    ): List<MemberBalance> {
        val groupExpenses = expenses.filter { it.groupId == groupId }

        require(groupExpenses.all { it.amountMinorUnits >= 0L }) {
            "Expense amounts cannot be negative."
        }

        val expenseIds = groupExpenses.map { it.id }.toSet()

        val groupSplits = splits.filter { it.expenseId in expenseIds }

        require(groupSplits.all { it.shareMinorUnits >= 0L }) {
            "Expense shares cannot be negative."
        }

        val splitsByExpense = groupSplits.groupBy { it.expenseId }

        val paidByMember = mutableMapOf<Long, Long>()
        val owedByMember = mutableMapOf<Long, Long>()

        for (expense in groupExpenses) {
            val expenseSplits = splitsByExpense[expense.id].orEmpty()

            require(expenseSplits.isNotEmpty()) {
                "Expense ${expense.id} must have at least one split."
            }

            val totalShares = expenseSplits.fold(0L) { total, split ->
                Math.addExact(total, split.shareMinorUnits)
            }

            require(totalShares == expense.amountMinorUnits) {
                "Expense ${expense.id} shares ($totalShares) must equal " +
                    "expense amount (${expense.amountMinorUnits})."
            }

            addAmount(
                paidByMember,
                expense.paidByMemberId,
                expense.amountMinorUnits
            )

            for (split in expenseSplits) {
                addAmount(
                    owedByMember,
                    split.memberId,
                    split.shareMinorUnits
                )
            }
        }

        val memberIds = buildSet {
            addAll(memberNames.keys)
            addAll(paidByMember.keys)
            addAll(owedByMember.keys)
        }

        return memberIds
            .sorted()
            .map { memberId ->
                val paid = paidByMember[memberId] ?: 0L
                val owed = owedByMember[memberId] ?: 0L

                MemberBalance(
                    memberId = memberId,
                    memberName = memberNames[memberId] ?: "Unknown member",
                    totalPaidMinorUnits = paid,
                    totalOwedMinorUnits = owed
                )
            }
    }

    private fun addAmount(
        amounts: MutableMap<Long, Long>,
        memberId: Long,
        amount: Long
    ) {
        amounts[memberId] = Math.addExact(
            amounts[memberId] ?: 0L,
            amount
        )
    }
}