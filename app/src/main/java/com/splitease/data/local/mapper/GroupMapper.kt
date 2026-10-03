package com.splitease.data.local.mapper

import com.splitease.data.local.dao.GroupWithStats
import com.splitease.data.local.entity.GroupEntity
import com.splitease.domain.model.Group

fun GroupEntity.toDomain(): Group = Group(
    id = id,
    name = name,
    description = description,
    currencyCode = currencyCode,
    createdAt = createdAt,
    updatedAt = updatedAt,
    budgetMinorUnits = budgetMinorUnits,
)

fun GroupWithStats.toDomain(): Group = Group(
    id = id,
    name = name,
    description = description,
    currencyCode = currencyCode,
    createdAt = createdAt,
    updatedAt = updatedAt,
    memberCount = member_count,
    totalAmountMinorUnits = total_amount,
    budgetMinorUnits = budget_minor_units,
)

fun Group.toEntity(): GroupEntity = GroupEntity(
    id = id,
    name = name,
    description = description,
    currencyCode = currencyCode,
    createdAt = createdAt,
    updatedAt = updatedAt,
    budgetMinorUnits = budgetMinorUnits,
)
