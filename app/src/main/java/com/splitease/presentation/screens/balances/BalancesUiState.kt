package com.splitease.presentation.screens.balances

import com.splitease.domain.model.MemberBalance
import com.splitease.presentation.screens.summary.AdjustedMemberBalance

/**
 * UI state for the Balances screen.
 *
 * [balances] holds the raw expense-ledger values used for Paid and Share rows —
 * these are never altered by settlement payments.
 *
 * [adjustedBalances] holds each member's CURRENT net position after applying
 * recorded settlement payments.  This is what drives the net-balance row and
 * the summary counters (creditorsCount / debtorsCount).
 *
 * All monetary values are Long minor units — no Double/Float.
 */
data class BalancesUiState(
    val isLoading: Boolean = true,
    val groupId: Long = 0L,
    val groupName: String = "",
    val currencyCode: String = "INR",
    /** Raw balances from CalculateMemberBalancesUseCase — used for Paid and Share rows only. */
    val balances: List<MemberBalance> = emptyList(),
    /** Per-member net positions after applying recorded settlement payments. */
    val adjustedBalances: List<AdjustedMemberBalance> = emptyList(),
    val groupNotFound: Boolean = false,
    val errorMessage: String? = null,
) {
    /** True when loading is complete, no error, and there are no expenses yet. */
    val isEmpty: Boolean
        get() = !isLoading && !groupNotFound && errorMessage == null
            && balances.all { it.totalPaidMinorUnits == 0L && it.totalOwedMinorUnits == 0L }

    /** Total paid across all members for a quick summary header (unaffected by payments). */
    val totalSpendMinorUnits: Long
        get() = balances.sumOf { it.totalPaidMinorUnits }

    /** Count of members still owed money (positive adjusted net). */
    val creditorsCount: Int
        get() = adjustedBalances.count { it.adjustedNetMinorUnits > 0L }

    /** Count of members who still owe money (negative adjusted net). */
    val debtorsCount: Int
        get() = adjustedBalances.count { it.adjustedNetMinorUnits < 0L }
}
