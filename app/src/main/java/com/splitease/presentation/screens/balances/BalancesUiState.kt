package com.splitease.presentation.screens.balances

import com.splitease.domain.model.MemberBalance

/**
 * UI state for the Balances screen.
 *
 * Balances are derived from [CalculateMemberBalancesUseCase] and are always
 * Long minor units — no Double/Float.
 */
data class BalancesUiState(
    val isLoading: Boolean = true,
    val groupId: Long = 0L,
    val groupName: String = "",
    val currencyCode: String = "INR",
    val balances: List<MemberBalance> = emptyList(),
    val groupNotFound: Boolean = false,
    val errorMessage: String? = null,
) {
    /** True when loading is complete, no error, and there are no expenses yet. */
    val isEmpty: Boolean
        get() = !isLoading && !groupNotFound && errorMessage == null
            && balances.all { it.totalPaidMinorUnits == 0L && it.totalOwedMinorUnits == 0L }

    /** Total paid across all members for a quick summary header. */
    val totalSpendMinorUnits: Long
        get() = balances.sumOf { it.totalPaidMinorUnits }

    /** Count of members who are owed money (positive net). */
    val creditorsCount: Int
        get() = balances.count { it.netMinorUnits > 0L }

    /** Count of members who owe money (negative net). */
    val debtorsCount: Int
        get() = balances.count { it.netMinorUnits < 0L }
}
