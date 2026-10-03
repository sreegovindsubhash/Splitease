package com.splitease.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.splitease.data.local.dao.ExpenseDao
import com.splitease.data.local.dao.ExpenseSplitDao
import com.splitease.data.local.dao.GroupDao
import com.splitease.data.local.dao.MemberDao
import com.splitease.data.local.dao.ReminderDao
import com.splitease.data.local.dao.SettlementPaymentDao
import com.splitease.data.local.entity.ExpenseEntity
import com.splitease.data.local.entity.ExpenseSplitEntity
import com.splitease.data.local.entity.GroupEntity
import com.splitease.data.local.entity.MemberEntity
import com.splitease.data.local.entity.ReminderEntity
import com.splitease.data.local.entity.SettlementPaymentEntity

@Database(
    entities = [
        GroupEntity::class,
        MemberEntity::class,
        ExpenseEntity::class,
        ExpenseSplitEntity::class,
        SettlementPaymentEntity::class,
        ReminderEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun groupDao(): GroupDao
    abstract fun memberDao(): MemberDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun expenseSplitDao(): ExpenseSplitDao
    abstract fun settlementPaymentDao(): SettlementPaymentDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        private const val DATABASE_NAME = "splitease.db"

        /**
         * Version 1 → 2: adds the settlement_payments table.
         * Existing data is unaffected.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `settlement_payments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `group_id` INTEGER NOT NULL,
                        `debtor_member_id` INTEGER NOT NULL,
                        `creditor_member_id` INTEGER NOT NULL,
                        `amount_minor_units` INTEGER NOT NULL,
                        `paid_at` INTEGER NOT NULL,
                        FOREIGN KEY(`group_id`) REFERENCES `groups`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`debtor_member_id`) REFERENCES `members`(`id`) ON DELETE RESTRICT,
                        FOREIGN KEY(`creditor_member_id`) REFERENCES `members`(`id`) ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_settlement_payments_group_id` ON `settlement_payments` (`group_id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_settlement_payments_debtor_member_id` ON `settlement_payments` (`debtor_member_id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_settlement_payments_creditor_member_id` ON `settlement_payments` (`creditor_member_id`)"
                )
            }
        }

        /**
         * Version 2 → 3: normalises the `category` column in `expenses`.
         * Existing rows that hold 'UTILITIES' or 'EDUCATION' (from the old enum)
         * are updated to 'OTHER' so they map cleanly to the current ExpenseCategory enum.
         * All other category values are left unchanged.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "UPDATE `expenses` SET `category` = 'OTHER' WHERE `category` IN ('UTILITIES', 'EDUCATION')"
                )
            }
        }

        /**
         * Version 3 → 4: adds optional budget_minor_units column to `groups`.
         * Existing groups receive NULL (no budget configured). No data is lost.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `groups` ADD COLUMN `budget_minor_units` INTEGER DEFAULT NULL"
                )
            }
        }

        /**
         * Version 4 → 5: adds the `reminders` table.
         * Existing data in all other tables is unaffected.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reminders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `scheduled_at` INTEGER NOT NULL,
                        `note` TEXT NOT NULL DEFAULT '',
                        `is_completed` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME,
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }
    }
}
