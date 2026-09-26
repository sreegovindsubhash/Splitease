package com.splitease.presentation.screens.expenses

import com.splitease.domain.model.Expense
import com.splitease.domain.model.Member

/**
 * UI state for the Expenses list screen.
 */
data class ExpensesUiState(
    val isLoading: Boolean = true,
    val expenses: List<Expense> = emptyList(),
    val members: List<Member> = emptyList(),   // for showing payer name per row
    val groupId: Long = 0L,
    val currencyCode: String = "INR",
    val groupName: String = "",
    val groupNotFound: Boolean = false,
    val errorMessage: String? = null,

    // Delete confirmation
    val expensePendingDelete: Expense? = null,
) {
    val isEmpty: Boolean get() = !isLoading && expenses.isEmpty() && !groupNotFound

    fun payerName(paidByMemberId: Long): String =
        members.firstOrNull { it.id == paidByMemberId }?.name ?: "Unknown"
}
