package com.splitease.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.splitease.data.local.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Query(
        """
        SELECT g.*,
               COALESCE(mc.member_count, 0) AS member_count,
               COALESCE(ec.total_amount, 0) AS total_amount
        FROM groups g
        LEFT JOIN (
            SELECT group_id, COUNT(*) AS member_count
            FROM members
            GROUP BY group_id
        ) mc ON mc.group_id = g.id
        LEFT JOIN (
            SELECT group_id, SUM(amount_minor_units) AS total_amount
            FROM expenses
            GROUP BY group_id
        ) ec ON ec.group_id = g.id
        ORDER BY g.updated_at DESC
        """
    )
    fun getGroupsWithStats(): Flow<List<GroupWithStats>>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getGroupById(id: Long): GroupEntity?

    @Query(
        """
        SELECT g.*,
               COALESCE(mc.member_count, 0) AS member_count,
               COALESCE(ec.total_amount, 0) AS total_amount
        FROM groups g
        LEFT JOIN (
            SELECT group_id, COUNT(*) AS member_count
            FROM members
            GROUP BY group_id
        ) mc ON mc.group_id = g.id
        LEFT JOIN (
            SELECT group_id, SUM(amount_minor_units) AS total_amount
            FROM expenses
            GROUP BY group_id
        ) ec ON ec.group_id = g.id
        WHERE g.id = :id
        """
    )
    fun getGroupWithStatsById(id: Long): Flow<GroupWithStats?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGroup(group: GroupEntity): Long

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Delete
    suspend fun deleteGroup(group: GroupEntity)
}

/** Lightweight projection used by the groups list screen. */
data class GroupWithStats(
    val id: Long,
    val name: String,
    val description: String,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    val member_count: Int,
    val total_amount: Long,
    @ColumnInfo(name = "budget_minor_units") val budget_minor_units: Long? = null,
)
