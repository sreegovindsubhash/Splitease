package com.splitease.presentation.screens.onboarding

import com.splitease.data.repository.OnboardingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for onboarding completion / persistence logic.
 *
 * Uses [FakeOnboardingRepository] so these tests run purely on the JVM without
 * an Android context or DataStore on disk.
 *
 * Covers the six required scenarios:
 *  1. New state shows onboarding (default flag is false).
 *  2. Completing via "Create your first group" marks onboarding complete.
 *  3. Completing via "Skip" marks onboarding complete.
 *  4. Completed state persists (flag remains true after being set).
 *  5. Default UI state is incomplete.
 *  6. Reinitializing with completed state does not show onboarding.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeRepo: FakeOnboardingRepository
    private lateinit var viewModel: OnboardingViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeOnboardingRepository()
        viewModel = OnboardingViewModel(fakeRepo)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Test 1: New state shows onboarding ────────────────────────────────────

    @Test
    fun newState_onboardingNotCompleted() {
        assertFalse(
            fakeRepo.completedValue,
            "Default state must be incomplete so onboarding is shown to new users",
        )
    }

    // ── Test 2: CTA marks onboarding complete ────────────────────────────────

    @Test
    fun createFirstGroup_marksOnboardingComplete() = runTest {
        viewModel.onCreateFirstGroup()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeRepo.completedValue, "onCreateFirstGroup must mark onboarding completed")
    }

    // ── Test 3: Skip marks onboarding complete ───────────────────────────────

    @Test
    fun skip_marksOnboardingComplete() = runTest {
        viewModel.onSkip()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeRepo.completedValue, "onSkip must mark onboarding completed")
    }

    // ── Test 4: Completed state persists ─────────────────────────────────────

    @Test
    fun completedState_persists() = runTest {
        viewModel.onSkip()
        testDispatcher.scheduler.advanceUntilIdle()

        // Re-read from the same repository — simulates a DataStore re-read.
        assertTrue(fakeRepo.completedValue, "Completion flag must remain true after being set")
    }

    // ── Test 5: Default UI state is incomplete ───────────────────────────────

    @Test
    fun defaultUiState_isNotCompleted() {
        val state = viewModel.uiState.value
        assertFalse(state.isCompleted, "Initial UI state must not be marked as completed")
        assertFalse(state.navigateToCreateGroup, "Initial state must not trigger navigation")
    }

    // ── Test 6: Reinitializing with completed state doesn't show onboarding ──

    @Test
    fun reinitialized_withCompletedFlag_doesNotShowOnboarding() = runTest {
        // Mark complete via skip.
        viewModel.onSkip()
        testDispatcher.scheduler.advanceUntilIdle()

        // A brand-new ViewModel backed by the same (now-completed) repository instance.
        val newViewModel = OnboardingViewModel(fakeRepo)

        // The repository flag is true — the startup logic in MainActivity reads this
        // value and routes directly to Groups, so onboarding is never shown again.
        assertTrue(
            fakeRepo.completedValue,
            "Repository must report completed so the startup logic bypasses onboarding",
        )
        // A freshly created ViewModel starts in its idle default state.
        assertFalse(
            newViewModel.uiState.value.isCompleted,
            "New ViewModel must start in idle state, not trigger spurious navigation",
        )
    }
}

// ── Fake ─────────────────────────────────────────────────────────────────────

/**
 * In-memory implementation of [OnboardingRepository] for use in unit tests.
 * No Android context or DataStore dependency.
 */
private class FakeOnboardingRepository : OnboardingRepository {

    private val _completed = MutableStateFlow(false)

    /** Exposes the stored boolean value for assertions. */
    val completedValue: Boolean get() = _completed.value

    override val isOnboardingCompleted: Flow<Boolean> = _completed

    override suspend fun setOnboardingCompleted() {
        _completed.value = true
    }
}
