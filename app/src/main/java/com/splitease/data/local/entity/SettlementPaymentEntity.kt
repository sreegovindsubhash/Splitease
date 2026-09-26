package com.splitease.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted record of a settlement payment.
 *
 * Foreign keys:
 *   - group_id → groups(id)  CASCADE delete: payments are removed when the group is deleted.
 *   - debtor_member_id, creditor_member_id → members(id)  RESTRICT: prevent deleting a member
 *     that has recorded payments. The UI must handle this scenario.
 *
 * Note: member FKs use RESTRICT intentionally so that data is not silently lost if a member
 * is removed while they have payment records. The member-management screen should warn the user.
 */
@Entity(
    tableName = "settlement_payments",
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
            childColumns = ["debtor_member_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["creditor_member_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("group_id"),
        Index("debtor_member_id"),
        Index("creditor_member_id"),
    ],
)
data class SettlementPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "group_id")
    val groupId: Long,

    @ColumnInfo(name = "debtor_member_id")
    val debtorMemberId: Long,

    @ColumnInfo(name = "creditor_member_id")
    val creditorMemberId: Long,

    /** Amount in minor units (paise, cents). Always > 0. Never Double/Float. */
    @ColumnInfo(name = "amount_minor_units")
    val amountMinorUnits: Long,

    /** Unix epoch ms when the payment was recorded by the user. */
    @ColumnInfo(name = "paid_at")
    val paidAt: Long,
)
