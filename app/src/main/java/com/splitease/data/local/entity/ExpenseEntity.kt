package com.splitease.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["paid_by_member_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("group_id"), Index("paid_by_member_id")],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "group_id")
    val groupId: Long,

    @ColumnInfo(name = "description")
    val description: String,

    /** Amount stored as Long minor units (e.g. paise). NEVER Double. */
    @ColumnInfo(name = "amount_minor_units")
    val amountMinorUnits: Long,

    @ColumnInfo(name = "currency_code")
    val currencyCode: String,

    @ColumnInfo(name = "paid_by_member_id")
    val paidByMemberId: Long,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "date")
    val date: Long,

    @ColumnInfo(name = "note")
    val note: String = "",

    @ColumnInfo(name = "split_method")
    val splitMethod: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)
