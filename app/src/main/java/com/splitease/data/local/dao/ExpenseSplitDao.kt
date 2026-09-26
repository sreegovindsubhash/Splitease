package com.splitease.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.splitease.data.local.entity.ExpenseSplitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseSplitDao {

    @Query("SELECT * FROM expense_splits WHERE expense_id = :expenseId")
    fun getSplitsForExpense(expenseId: Long): Flow<List<ExpenseSplitEntity>>

    @Query(
        """
        SELECT es.*
        FROM expense_splits es
        INNER JOIN expenses e ON e.id = es.expense_id
        WHERE e.group_id = :groupId
        """
    )
    fun getSplitsForGroup(groupId: Long): Flow<List<ExpenseSplitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSplits(splits: List<ExpenseSplitEntity>)

    @Query("DELETE FROM expense_splits WHERE expense_id = :expenseId")
    suspend fun deleteSplitsForExpense(expenseId: Long)

    /**
     * Returns each member's total owed (sum of their splits) for all expenses
     * in the given group. Used by balance calculation.
     * All values are Long minor units — no floating-point arithmetic.
     */
    @Query(
        """
        SELECT es.member_id,
               COALESCE(SUM(es.share_minor_units), 0) AS total_owed_minor_units
        FROM expense_splits es
        INNER JOIN expenses e ON e.id = es.expense_id
        WHERE e.group_id = :groupId
        GROUP BY es.member_id
        """
    )
    suspend fun getTotalOwedByMemberForGroup(groupId: Long): List<MemberOwed>
}

/** Projection: how much a single member owes in total across a group's expenses. */
data class MemberOwed(
    @ColumnInfo(name = "member_id") val memberId: Long,
    @ColumnInfo(name = "total_owed_minor_units") val totalOwedMinorUnits: Long,
)
