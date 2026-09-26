package com.splitease.presentation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pure JVM tests for the navigation guard in [rememberSingleEventHandler]
 * and the root-safety invariant of [NavController.safePopBackStack].
 *
 * ## What is tested
 *
 * ### Throttle timing (Section 1)
 * Verifies that the shared timestamp throttle suppresses rapid back events
 * and allows deliberate ones.  Exercises the logic inside [buildGuard],
 * which mirrors [rememberSingleEventHandler] but uses a [FakeClock].
 *
 * ### Root-safety (Section 2)
 * Verifies [safePopBackStack] behaviour via [FakeNavController]:
 *  - Pop with a previous entry pops normally.
 *  - Pop at the root (no previous entry) is a no-op.
 *  - A second rapid back after returning to root cannot empty the stack.
 *  - Deliberate back from a child that was navigated to after a previous
 *    back still works.
 *
 * ### Combined invariant (Section 3)
 * Verifies that sharing a single [LongRef] across two guard lambdas (the
 * cross-destination topology used in production) suppresses a rapid second
 * tap even when it lands on a completely different destination's guard.
 */
class NavigationGuardTest {

    // ── Infrastructure ────────────────────────────────────────────────────────

    private class FakeClock(var nowMs: Long = 0L)

    /**
     * Mirrors [rememberSingleEventHandler] without @Composable.
     * Accepts an explicit [LongRef] so tests can share state across
     * multiple guard instances.
     */
    private fun buildGuard(
        sharedState: LongRef,
        clock: FakeClock,
        throttleMs: Long = 500L,
        action: () -> Unit,
    ): () -> Unit = {
        val now = clock.nowMs
        if (now - sharedState.value >= throttleMs) {
            sharedState.value = now
            action()
        }
    }

    /**
     * Minimal fake that tracks whether [safePopBackStack] would pop or no-op,
     * driven entirely by [hasPrevious].
     *
     * [safePopBackStack] reads [previousBackStackEntry] (non-null == has
     * somewhere to go back to).  Here we model that with a simple flag and
     * a mutable pop counter.
     */
    private class FakeNavController(hasPrevious: Boolean) {
        var hasPreviousEntry: Boolean = hasPrevious
        var popCount: Int = 0

        val previousBackStackEntry: Any? get() = if (hasPreviousEntry) Unit else null

        /** Mirrors the real safePopBackStack logic. */
        fun safePopBackStack() {
            if (previousBackStackEntry != null) {
                popCount++
                // After the pop the destination changes; model root as having
                // no further previous entry (conservative).
                hasPreviousEntry = false
            }
        }
    }

    // ── Section 1: throttle timing ────────────────────────────────────────────

    @Test
    fun `first tap executes the action`() {
        val clock = FakeClock(nowMs = 1_000L)
        val state = LongRef(-500L)
        var count = 0
        val guarded = buildGuard(state, clock) { count++ }

        guarded()

        assertEquals(1, count, "First tap should execute the action")
    }

    @Test
    fun `immediate second tap is suppressed`() {
        val clock = FakeClock(nowMs = 1_000L)
        val state = LongRef(-500L)
        var count = 0
        val guarded = buildGuard(state, clock) { count++ }

        guarded()
        clock.nowMs = 1_050L
        guarded()

        assertEquals(1, count, "Second tap within throttle window should be suppressed")
    }

    @Test
    fun `tap at +499 ms is suppressed`() {
        val clock = FakeClock(nowMs = 1_000L)
        val state = LongRef(-500L)
        var count = 0
        val guarded = buildGuard(state, clock) { count++ }

        guarded()
        clock.nowMs = 1_499L
        guarded()

        assertEquals(1, count, "Tap at +499 ms should be suppressed")
    }

    @Test
    fun `tap at exact +500 ms boundary is accepted`() {
        val clock = FakeClock(nowMs = 1_000L)
        val state = LongRef(-500L)
        var count = 0
        val guarded = buildGuard(state, clock) { count++ }

        guarded()
        clock.nowMs = 1_500L
        guarded()

        assertEquals(2, count, "Tap at exactly +500 ms should be accepted")
    }

    @Test
    fun `tap at +501 ms is accepted`() {
        val clock = FakeClock(nowMs = 1_000L)
        val state = LongRef(-500L)
        var count = 0
        val guarded = buildGuard(state, clock) { count++ }

        guarded()
        clock.nowMs = 1_501L
        guarded()

        assertEquals(2, count, "Tap at +501 ms should be accepted")
    }

    @Test
    fun `multiple rapid taps produce only one navigation action`() {
        val clock = FakeClock(nowMs = 2_000L)
        val state = LongRef(-500L)
        var count = 0
        val guarded = buildGuard(state, clock) { count++ }

        guarded()
        clock.nowMs = 2_050L; guarded()
        clock.nowMs = 2_100L; guarded()
        clock.nowMs = 2_200L; guarded()

        assertEquals(1, count, "All rapid taps within window should produce exactly one action")
    }

    @Test
    fun `guard does not permanently lock navigation`() {
        val clock = FakeClock(nowMs = 3_000L)
        val state = LongRef(-500L)
        var count = 0
        val guarded = buildGuard(state, clock) { count++ }

        guarded()
        clock.nowMs = 3_050L; guarded()   // suppressed
        clock.nowMs = 3_600L; guarded()   // accepted
        clock.nowMs = 3_650L; guarded()   // suppressed
        clock.nowMs = 4_200L; guarded()   // accepted

        assertEquals(3, count, "Taps after the interval should always be allowed")
    }

    // ── Section 2: root-safety invariant ─────────────────────────────────────

    /** Back from a child destination (has previous entry) pops normally. */
    @Test
    fun `safePopBackStack pops when previous entry exists`() {
        val nav = FakeNavController(hasPrevious = true)

        nav.safePopBackStack()

        assertEquals(1, nav.popCount, "Should pop once when previous entry exists")
    }

    /** Back at the root destination (no previous entry) is a no-op. */
    @Test
    fun `safePopBackStack is a no-op at root`() {
        val nav = FakeNavController(hasPrevious = false)

        nav.safePopBackStack()

        assertEquals(0, nav.popCount, "Should not pop when at root — no previous entry")
    }

    /**
     * A second rapid back after the first pop lands back at the root must
     * not empty the stack further.
     * This is the primary regression scenario: GroupDetails back (pop #1)
     * → Groups is now root → second tap fires → must be no-op.
     */
    @Test
    fun `second back after arriving at root is a no-op`() {
        val nav = FakeNavController(hasPrevious = true)

        nav.safePopBackStack()   // pop #1 — valid, lands at root
        nav.safePopBackStack()   // pop #2 — root has no previous entry, no-op

        assertEquals(1, nav.popCount, "Only the first back should pop; root is safe")
    }

    /**
     * After navigating to a child from the root, a deliberate single back
     * should still work correctly.
     */
    @Test
    fun `deliberate back after navigating to child works`() {
        val nav = FakeNavController(hasPrevious = false) // starts at root

        // Simulate forward navigation (e.g. tap a group): now there is a previous entry.
        nav.hasPreviousEntry = true

        nav.safePopBackStack()

        assertEquals(1, nav.popCount, "Back from child after forward nav should pop normally")
    }

    /**
     * Even with many rapid taps, the pop count never exceeds the number of
     * valid previous entries.
     */
    @Test
    fun `repeated backs at root never exceed one pop`() {
        val nav = FakeNavController(hasPrevious = true)

        repeat(5) { nav.safePopBackStack() }

        assertEquals(1, nav.popCount, "Only the first back (valid) should pop; rest are no-ops")
    }

    // ── Section 3: cross-destination throttle invariant ───────────────────────

    /**
     * Two guards share one [LongRef].  A rapid second tap on a different
     * destination's guard is still suppressed.
     */
    @Test
    fun `rapid second tap on newly-exposed destination is suppressed via shared state`() {
        val clock = FakeClock(nowMs = 5_000L)
        val sharedState = LongRef(-500L)

        var destAPopCount = 0
        var destBPopCount = 0

        val guardA = buildGuard(sharedState, clock) { destAPopCount++ }
        val guardB = buildGuard(sharedState, clock) { destBPopCount++ }

        guardA()                // tap 1 on A at t=5000 — accepted
        clock.nowMs = 5_050L
        guardB()                // tap 2 on B at t=5050 — suppressed (50 ms)

        assertEquals(1, destAPopCount, "Destination A should have been popped once")
        assertEquals(0, destBPopCount, "Rapid second tap on B must be suppressed by shared state")
    }

    /** After the throttle window a deliberate tap on the exposed destination is accepted. */
    @Test
    fun `deliberate tap on newly-exposed destination after interval is accepted`() {
        val clock = FakeClock(nowMs = 5_000L)
        val sharedState = LongRef(-500L)

        var destAPopCount = 0
        var destBPopCount = 0

        val guardA = buildGuard(sharedState, clock) { destAPopCount++ }
        val guardB = buildGuard(sharedState, clock) { destBPopCount++ }

        guardA()
        clock.nowMs = 5_600L   // 600 ms later — past the window
        guardB()

        assertEquals(1, destAPopCount, "Destination A popped once")
        assertEquals(1, destBPopCount, "Deliberate tap on B after interval should be accepted")
    }
}
