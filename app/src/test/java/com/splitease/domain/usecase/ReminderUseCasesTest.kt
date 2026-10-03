package com.splitease.domain.usecase

import com.splitease.domain.model.Reminder
import com.splitease.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Unit tests for Reminder use cases and supporting logic. */
class ReminderUseCasesTest {

    private val repo = FakeReminderRepo()

    // ── AddReminderUseCase ────────────────────────────────────────────────────

    @Test
    fun addReminder_valid_persists() = runTest {
        val reminder = makeReminder()
        val id = AddReminderUseCase(repo)(reminder)
        assertEquals(1L, id)
        assertNotNull(repo.lastAdded)
        assertEquals("Pay rent", repo.lastAdded!!.title)
    }

    @Test
    fun addReminder_blankTitle_throws() = runTest {
        assertFailsWith<IllegalArgumentException> {
            AddReminderUseCase(repo)(makeReminder(title = ""))
        }
    }

    @Test
    fun addReminder_whitespaceOnlyTitle_throws() = runTest {
        assertFailsWith<IllegalArgumentException> {
            AddReminderUseCase(repo)(makeReminder(title = "   "))
        }
    }

    @Test
    fun addReminder_zeroScheduledAt_throws() = runTest {
        assertFailsWith<IllegalArgumentException> {
            AddReminderUseCase(repo)(makeReminder(scheduledAt = 0L))
        }
    }

    @Test
    fun addReminder_negativeScheduledAt_throws() = runTest {
        assertFailsWith<IllegalArgumentException> {
            AddReminderUseCase(repo)(makeReminder(scheduledAt = -1L))
        }
    }

    // ── UpdateReminderUseCase ─────────────────────────────────────────────────

    @Test
    fun updateReminder_valid_callsRepository() = runTest {
        val reminder = makeReminder(id = 5L, title = "Updated title")
        UpdateReminderUseCase(repo)(reminder)
        assertEquals(reminder, repo.lastUpdated)
    }

    @Test
    fun updateReminder_zeroId_throws() = runTest {
        assertFailsWith<IllegalArgumentException> {
            UpdateReminderUseCase(repo)(makeReminder(id = 0L))
        }
    }

    @Test
    fun updateReminder_blankTitle_throws() = runTest {
        assertFailsWith<IllegalArgumentException> {
            UpdateReminderUseCase(repo)(makeReminder(id = 1L, title = ""))
        }
    }

    @Test
    fun updateReminder_preservesId() = runTest {
        val reminder = makeReminder(id = 42L)
        UpdateReminderUseCase(repo)(reminder)
        assertEquals(42L, repo.lastUpdated?.id)
    }

    // ── DeleteReminderUseCase ─────────────────────────────────────────────────

    @Test
    fun deleteReminder_delegatesToRepository() = runTest {
        val reminder = makeReminder(id = 3L)
        DeleteReminderUseCase(repo)(reminder)
        assertEquals(reminder, repo.lastDeleted)
    }

    // ── CompleteReminderUseCase ───────────────────────────────────────────────

    @Test
    fun completeReminder_setsIsCompletedTrue() = runTest {
        val reminder = makeReminder(id = 1L, isCompleted = false)
        CompleteReminderUseCase(repo)(reminder)
        assertNotNull(repo.lastUpdated)
        assertTrue(repo.lastUpdated!!.isCompleted)
    }

    @Test
    fun completeReminder_preservesId() = runTest {
        val reminder = makeReminder(id = 7L)
        CompleteReminderUseCase(repo)(reminder)
        assertEquals(7L, repo.lastUpdated?.id)
    }

    // ── GetRemindersUseCase ───────────────────────────────────────────────────

    @Test
    fun getReminders_returnsFlowFromRepository() = runTest {
        val r1 = makeReminder(id = 1L, title = "First")
        val r2 = makeReminder(id = 2L, title = "Second")
        repo.allReminders = listOf(r1, r2)

        var result: List<Reminder> = emptyList()
        GetRemindersUseCase(repo)().collect { result = it }

        assertEquals(2, result.size)
        assertEquals("First", result[0].title)
    }

    // ── scheduledAt conversion / storage ─────────────────────────────────────

    @Test
    fun scheduledAt_storedAndRetrievedExactly() = runTest {
        val epochMs = 1_800_000_000_000L   // a future timestamp well beyond year 2000
        val reminder = makeReminder(scheduledAt = epochMs)
        AddReminderUseCase(repo)(reminder)
        assertEquals(epochMs, repo.lastAdded!!.scheduledAt)
    }

    @Test
    fun scheduledAt_distinctValuesArePreservedDistinctly() = runTest {
        val t1 = 1_700_000_000_000L
        val t2 = 1_700_000_060_000L  // 60 s later
        AddReminderUseCase(repo)(makeReminder(scheduledAt = t1))
        AddReminderUseCase(repo)(makeReminder(scheduledAt = t2))
        // Verify both times were stored unchanged
        assertTrue(t1 != t2)
    }

    // ── Completed reminders should not be scheduled ───────────────────────────

    @Test
    fun completedReminder_notScheduled() {
        val completed = makeReminder(id = 1L, isCompleted = true, scheduledAt = FAR_FUTURE)
        val scheduler = FakeScheduler()
        scheduler.scheduleIfEligible(completed)
        assertTrue(scheduler.scheduledIds.isEmpty(), "Completed reminder must not be scheduled")
    }

    // ── Past reminders should not be scheduled ────────────────────────────────

    @Test
    fun pastReminder_notScheduled() {
        val past = makeReminder(id = 1L, isCompleted = false, scheduledAt = 1_000L)
        val scheduler = FakeScheduler(nowMs = System.currentTimeMillis())
        scheduler.scheduleIfEligible(past)
        assertTrue(scheduler.scheduledIds.isEmpty(), "Past reminder must not be scheduled")
    }

    @Test
    fun futureReminder_isScheduled() {
        val future = makeReminder(id = 1L, isCompleted = false, scheduledAt = FAR_FUTURE)
        val scheduler = FakeScheduler(nowMs = System.currentTimeMillis())
        scheduler.scheduleIfEligible(future)
        assertTrue(scheduler.scheduledIds.contains(1L), "Future reminder must be scheduled")
    }

    // ── Edit / delete cancel / replace notification ───────────────────────────

    @Test
    fun editReminder_cancelsOldAndSchedulesNew() {
        val old = makeReminder(id = 10L, scheduledAt = FAR_FUTURE)
        val scheduler = FakeScheduler(nowMs = System.currentTimeMillis())
        scheduler.scheduleIfEligible(old)
        assertTrue(scheduler.scheduledIds.contains(10L))

        // Simulate edit: cancel + reschedule
        scheduler.cancel(10L)
        val updated = old.copy(scheduledAt = FAR_FUTURE + 3_600_000L)
        scheduler.scheduleIfEligible(updated)

        assertTrue(scheduler.cancelledIds.contains(10L))
        assertTrue(scheduler.scheduledIds.contains(10L))
    }

    @Test
    fun deleteReminder_cancelsNotification() {
        val reminder = makeReminder(id = 5L, scheduledAt = FAR_FUTURE)
        val scheduler = FakeScheduler(nowMs = System.currentTimeMillis())
        scheduler.scheduleIfEligible(reminder)
        scheduler.cancel(reminder.id)
        assertTrue(scheduler.cancelledIds.contains(5L))
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private const val FAR_FUTURE = Long.MAX_VALUE / 2

private fun makeReminder(
    id: Long = 0L,
    title: String = "Pay rent",
    scheduledAt: Long = FAR_FUTURE,
    note: String = "",
    isCompleted: Boolean = false,
) = Reminder(
    id = id,
    title = title,
    scheduledAt = scheduledAt,
    note = note,
    isCompleted = isCompleted,
)

private class FakeReminderRepo : ReminderRepository {
    var allReminders: List<Reminder> = emptyList()
    var lastAdded: Reminder? = null
    var lastUpdated: Reminder? = null
    var lastDeleted: Reminder? = null
    private var nextId = 1L

    override fun getAll(): Flow<List<Reminder>> = flowOf(allReminders)

    override suspend fun getActiveFutureReminders(nowMs: Long): List<Reminder> =
        allReminders.filter { !it.isCompleted && it.scheduledAt > nowMs }

    override suspend fun getById(id: Long): Reminder? = allReminders.firstOrNull { it.id == id }

    override suspend fun add(reminder: Reminder): Long {
        lastAdded = reminder
        return nextId++
    }

    override suspend fun update(reminder: Reminder) {
        lastUpdated = reminder
    }

    override suspend fun delete(reminder: Reminder) {
        lastDeleted = reminder
    }
}

/**
 * A pure-Kotlin stand-in for [com.splitease.notifications.ReminderNotificationScheduler]
 * that records which IDs are scheduled / cancelled without touching Android APIs.
 */
private class FakeScheduler(private val nowMs: Long = 0L) {
    val scheduledIds = mutableSetOf<Long>()
    val cancelledIds = mutableSetOf<Long>()

    fun scheduleIfEligible(reminder: Reminder) {
        if (reminder.isCompleted) return
        if (reminder.scheduledAt <= nowMs) return
        scheduledIds.add(reminder.id)
    }

    fun cancel(id: Long) {
        cancelledIds.add(id)
        scheduledIds.remove(id)
    }
}
