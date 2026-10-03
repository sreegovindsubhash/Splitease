package com.splitease.domain.model

/**
 * Domain model for an Expense.
 * [amountMinorUnits] is stored as Long minor units (e.g. paise, cents).
 */
data class Expense(
    val id: Long = 0,
    val groupId: Long,
    val description: String,
    val amountMinorUnits: Long,
    val currencyCode: String,
    val paidByMemberId: Long,
    val category: ExpenseCategory,
    val date: Long,
    val note: String = "",
    val splitMethod: SplitMethod,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

enum class ExpenseCategory {
    FOOD,
    TRANSPORT,
    ACCOMMODATION,
    SHOPPING,
    ENTERTAINMENT,
    BILLS,
    OTHER,
}

enum class SplitMethod {
    EQUAL,
    EXACT,
    PERCENTAGE,
    SHARES,
}
