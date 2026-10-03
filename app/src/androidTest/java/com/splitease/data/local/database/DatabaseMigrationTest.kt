package com.splitease.data.local.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Instrumented migration tests for [AppDatabase].
 *
 * Each test verifies that the named migration runs without error and that
 * existing data survives intact.  These tests run on-device or in the emulator.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    /**
     * Verifies that MIGRATION_4_5 creates the `reminders` table and that
     * data inserted in schema version 4 is unaffected.
     */
    @Test
    fun migration4To5_createsRemindersTable_existingDataUnchanged() {
        // ── Set up a v4 database with a group row ─────────────────────────────
        helper.createDatabase(TEST_DB, 4).use { db ->
            db.execSQL(
                """
                INSERT INTO `groups` (id, name, description, currency_code, created_at, updated_at)
                VALUES (1, 'Trip', 'Holiday', 'INR', 1000, 1000)
                """.trimIndent()
            )
        }

        // ── Run the migration ─────────────────────────────────────────────────
        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            5,
            true,
            AppDatabase.MIGRATION_4_5,
        )

        // ── Verify: existing group row survives ───────────────────────────────
        db.query("SELECT name FROM groups WHERE id = 1").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("Trip", cursor.getString(0))
        }

        // ── Verify: reminders table exists and is empty ───────────────────────
        db.query("SELECT COUNT(*) FROM reminders").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }

        // ── Verify: we can insert a row into reminders ────────────────────────
        db.execSQL(
            """
            INSERT INTO `reminders` (title, scheduled_at, note, is_completed)
            VALUES ('Test', 9999999999999, '', 0)
            """.trimIndent()
        )
        db.query("SELECT COUNT(*) FROM reminders").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
    }
}
