package com.splitease.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expense_splits",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseEntity::class,
            parentColumns = ["id"],
            childColumns = ["expense_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["member_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("expense_id"), Index("member_id")],
)
data class ExpenseSplitEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "expense_id")
    val expenseId: Long,

    @ColumnInfo(name = "member_id")
    val memberId: Long,

    /** Share stored as Long minor units. sum(shares) must equal expense.amountMinorUnits. */
    @ColumnInfo(name = "share_minor_units")
    val shareMinorUnits: Long,
)
