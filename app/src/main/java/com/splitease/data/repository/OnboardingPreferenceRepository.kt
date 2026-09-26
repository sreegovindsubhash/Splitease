package com.splitease.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** DataStore delegate — one instance per process. */
private val Context.onboardingDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "splitease_onboarding_prefs")

/**
 * Contract for persisting the onboarding completion flag.
 * The interface keeps [OnboardingViewModel] testable without Android dependencies.
 */
interface OnboardingRepository {
    /** Emits `true` when onboarding has been completed/skipped, `false` otherwise. */
    val isOnboardingCompleted: Flow<Boolean>
    /** Marks onboarding as completed. */
    suspend fun setOnboardingCompleted()
}

/**
 * Persists and retrieves the onboarding completion flag using DataStore Preferences.
 *
 * - Default: `false` (onboarding not completed — new users see the screen).
 * - Once the user completes or skips onboarding the flag is set to `true` and
 *   never shown again.
 * - No Room — this is a simple UI flag, not domain data.
 */
class OnboardingPreferenceRepository(private val context: Context) : OnboardingRepository {

    private val completedKey = booleanPreferencesKey("onboarding_completed")

    override val isOnboardingCompleted: Flow<Boolean> = context.onboardingDataStore.data
        .map { prefs -> prefs[completedKey] ?: false }

    override suspend fun setOnboardingCompleted() {
        context.onboardingDataStore.edit { prefs ->
            prefs[completedKey] = true
        }
    }
}
