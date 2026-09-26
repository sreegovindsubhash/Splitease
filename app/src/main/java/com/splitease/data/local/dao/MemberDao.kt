package com.splitease.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.splitease.data.local.entity.MemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {

    @Query("SELECT * FROM members WHERE group_id = :groupId ORDER BY created_at ASC")
    fun getMembersForGroup(groupId: Long): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun getMemberById(id: Long): MemberEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMember(member: MemberEntity): Long

    @Update
    suspend fun updateMember(member: MemberEntity)

    @Delete
    suspend fun deleteMember(member: MemberEntity)
}
