package com.splitease.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.splitease.data.local.dao.ExpenseDao
import com.splitease.data.local.dao.ExpenseSplitDao
import com.splitease.data.local.dao.GroupDao
import com.splitease.data.local.dao.MemberDao
import com.splitease.data.local.entity.ExpenseEntity
import com.splitease.data.local.entity.ExpenseSplitEntity
import com.splitease.data.local.entity.GroupEntity
import com.splitease.data.local.entity.MemberEntity

@Database(
    entities = [
        GroupEntity::class,
        MemberEntity::class,
        ExpenseEntity::class,
        ExpenseSplitEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun groupDao(): GroupDao
    abstract fun memberDao(): MemberDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun expenseSplitDao(): ExpenseSplitDao

    companion object {
        private const val DATABASE_NAME = "splitease.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { instance = it }
            }
    }
}
