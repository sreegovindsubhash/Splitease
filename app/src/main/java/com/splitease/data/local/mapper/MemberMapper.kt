package com.splitease.data.local.mapper

import com.splitease.data.local.entity.MemberEntity
import com.splitease.domain.model.Member

fun MemberEntity.toDomain(): Member = Member(
    id = id,
    groupId = groupId,
    name = name,
    email = email,
    createdAt = createdAt,
)

fun Member.toEntity(): MemberEntity = MemberEntity(
    id = id,
    groupId = groupId,
    name = name,
    email = email,
    createdAt = createdAt,
)
