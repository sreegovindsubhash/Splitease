package com.splitease.data.local.mapper

import com.splitease.data.local.entity.ExpenseEntity
import com.splitease.data.local.entity.ExpenseSplitEntity
import com.splitease.domain.model.Expense
import com.splitease.domain.model.ExpenseCategory
import com.splitease.domain.model.ExpenseSplit
import com.splitease.domain.model.SplitMethod

fun ExpenseEntity.toDomain(): Expense = Expense(
    id = id,
    groupId = groupId,
    description = description,
    amountMinorUnits = amountMinorUnits,
    currencyCode = currencyCode,
    paidByMemberId = paidByMemberId,
    category = ExpenseCategory.valueOf(category),
    date = date,
    note = note,
    splitMethod = SplitMethod.valueOf(splitMethod),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    groupId = groupId,
    description = description,
    amountMinorUnits = amountMinorUnits,
    currencyCode = currencyCode,
    paidByMemberId = paidByMemberId,
    category = category.name,
    date = date,
    note = note,
    splitMethod = splitMethod.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun ExpenseSplitEntity.toDomain(): ExpenseSplit = ExpenseSplit(
    id = id,
    expenseId = expenseId,
    memberId = memberId,
    shareMinorUnits = shareMinorUnits,
)

fun ExpenseSplit.toEntity(): ExpenseSplitEntity = ExpenseSplitEntity(
    id = id,
    expenseId = expenseId,
    memberId = memberId,
    shareMinorUnits = shareMinorUnits,
)
