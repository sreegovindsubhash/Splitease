package com.splitease.domain.usecase

import com.splitease.domain.model.Expense
import com.splitease.domain.model.ExpenseSplit
import com.splitease.domain.model.SettlementTransaction

/**
 * Converts a group's expenses, splits and member names into a minimal, deterministic
 * list of suggested settlement transactions.
 *
 * Delegates balance calculation to [CalculateMemberBalancesUseCase] and
 * debt simplification to [DebtSimplifier]. No financial logic lives here.
 *
 * Returns an empty list when there are no expenses or when all balances are zero.
 */
class GetSettlementsUseCase(
    private val calculateBalances: CalculateMemberBalancesUseCase = CalculateMemberBalancesUseCase(),
) {

    operator fun invoke(
        groupId: Long,
        expenses: List<Expense>,
        splits: List<ExpenseSplit>,
        memberNames: Map<Long, String>,
    ): List<SettlementTransaction> {
        // Early-exit: nothing to settle if there are no expenses
        if (expenses.isEmpty()) return emptyList()

        val balances = calculateBalances(
            groupId = groupId,
            expenses = expenses,
            splits = splits,
            memberNames = memberNames,
        )

        // Build DebtSimplifier.Balance list from MemberBalance domain models.
        // The simplifier requires balances that sum to zero, which CalculateMemberBalancesUseCase
        // guarantees by construction (total paid == total owed across all members).
        val simplifierInput = balances.map { mb ->
            DebtSimplifier.Balance(
                memberId = mb.memberId,
                amountMinor = mb.netMinorUnits,
            )
        }

        val transfers = DebtSimplifier.simplify(simplifierInput)

        return transfers.map { transfer ->
            SettlementTransaction(
                fromMemberId = transfer.fromMemberId,
                fromMemberName = memberNames[transfer.fromMemberId] ?: "Unknown",
                toMemberId = transfer.toMemberId,
                toMemberName = memberNames[transfer.toMemberId] ?: "Unknown",
                amountMinorUnits = transfer.amountMinor,
            )
        }
    }
}
