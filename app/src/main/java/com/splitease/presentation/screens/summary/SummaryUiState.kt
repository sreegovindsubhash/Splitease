package com.splitease.presentation.screens.summary

import com.splitease.domain.model.ExpenseCategory
import com.splitease.domain.model.MemberBalance
import com.splitease.domain.model.SettlementTransaction

/**
 * UI state for the Group Summary / Financial Overview screen.
 *
 * All monetary values are Long minor units — no Double/Float.
 *
 * Data sources:
 *  - [totalSpentMinorUnits]   : sum of expense amountMinorUnits via CalculateMemberBalancesUseCase
 *                               (not a JOIN — unaffected by settlement payments)
 *  - [balances]               : raw CalculateMemberBalancesUseCase output (used for totalSpent only)
 *  - [adjustedBalances]       : per-member net positions after subtracting recorded payments
 *  - [outstandingSettlements] : ApplySettlementPaymentsUseCase applied to GetSettlementsUseCase output
 *  - [recentExpenses]         : latest [RECENT_EXPENSE_LIMIT] expenses ordered newest-first
 */
data class SummaryUiState(
    val isLoading: Boolean = true,
    val groupId: Long = 0L,
    val groupName: String = "",
    val currencyCode: String = "INR",

    /**
     * Raw member balances from CalculateMemberBalancesUseCase.
     * Used ONLY for [totalSpentMinorUnits] — not displayed directly.
     * Settlement payments do not affect expenses, so this stays unchanged.
     */
    val balances: List<MemberBalance> = emptyList(),

    /**
     * Per-member net positions after accounting for recorded settlement payments.
     * This is what the Balances section displays.
     *
     * For each recorded payment (debtor → creditor, amount):
     *   debtor's adjustedNet  += amount  (moves toward 0 — less owed)
     *   creditor's adjustedNet -= amount  (moves toward 0 — less owed to them)
     *
     * Computed in SummaryViewModel; no new use case required.
     */
    val adjustedBalances: List<AdjustedMemberBalance> = emptyList(),

    /**
     * Outstanding settlement transactions after subtracting recorded payments.
     * Computed identically to SettlementScreen — no duplicate logic.
     */
    val outstandingSettlements: List<SettlementTransaction> = emptyList(),

    /**
     * The most recent [RECENT_EXPENSE_LIMIT] expenses, sorted newest-first.
     * Each item includes the payer's resolved display name.
     */
    val recentExpenses: List<RecentExpenseItem> = emptyList(),

    /**
     * Total spending per category, sorted by total descending.
     * Only categories with at least one expense are included.
     */
    val categoryTotals: List<CategoryTotal> = emptyList(),

    /** True once at least one expense exists for this group. */
    val hasExpenses: Boolean = false,

    val groupNotFound: Boolean = false,
    val errorMessage: String? = null,
) {
    // ── Derived metrics ────────────────────────────────────────────────────────

    /**
     * Total amount spent — the sum of all expense amounts.
     *
     * Derived from the balances' totalPaid (which equals the sum of expense amounts by construction
     * in CalculateMemberBalancesUseCase) — **not** from a SQL JOIN that could multiply rows.
     * This matches the value shown on Group Details.
     */
    val totalSpentMinorUnits: Long
        get() = balances.sumOf { it.totalPaidMinorUnits }

    /**
     * Sum of all outstanding (unresolved) settlement amounts after recorded payments.
     * Matches SettlementScreen's view of outstanding obligations.
     */
    val outstandingAmountMinorUnits: Long
        get() = outstandingSettlements.fold(0L) { acc, txn ->
            Math.addExact(acc, txn.amountMinorUnits)
        }

    /** Count of debts still to settle. */
    val debtsToSettleCount: Int
        get() = outstandingSettlements.size

    /** True when loading is complete, group found, no error, no expenses yet. */
    val isEmpty: Boolean
        get() = !isLoading && !groupNotFound && errorMessage == null && !hasExpenses

    /** True when expenses exist and all debts are fully settled. */
    val isAllSettled: Boolean
        get() = !isLoading && !groupNotFound && errorMessage == null
            && hasExpenses && outstandingSettlements.isEmpty()
}

/** Maximum number of recent expenses shown in the summary section. */
const val RECENT_EXPENSE_LIMIT = 5

/**
 * A display-ready snapshot of a recent expense for the summary list.
 * The payer name is resolved from the member map before placing into state.
 */
data class RecentExpenseItem(
    val id: Long,
    val description: String,
    val amountMinorUnits: Long,
    val payerName: String,
    /** Milliseconds epoch — milliseconds since Unix epoch, same as the Expense.date field. */
    val dateMillis: Long,
)

/**
 * A member's net balance after accounting for recorded settlement payments.
 *
 * [adjustedNetMinorUnits] > 0 → member is still owed money
 * [adjustedNetMinorUnits] < 0 → member still owes money
 * [adjustedNetMinorUnits] = 0 → fully settled
 *
 * All values are Long minor units — no Double/Float.
 */
data class AdjustedMemberBalance(
    val memberId: Long,
    val memberName: String,
    /** Net position after applying all recorded settlement payments. Long minor units. */
    val adjustedNetMinorUnits: Long,
)

/**
 * Total spending for a single expense category.
 * [totalMinorUnits] is the sum of all expense amounts in this category.
 */
data class CategoryTotal(
    val category: ExpenseCategory,
    val totalMinorUnits: Long,
)
