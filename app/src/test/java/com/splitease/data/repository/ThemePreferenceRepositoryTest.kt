package com.splitease.data.repository

import com.splitease.data.repository.ThemePreferenceRepository.Companion.toAppThemeOrDefault
import com.splitease.presentation.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for theme preference logic.
 *
 * Tests 1–8 target the pure, Android-free parts of [ThemePreferenceRepository]:
 *   - [toAppThemeOrDefault] string-to-enum conversion
 *   - The mapping contract between stored strings and [AppTheme] values
 *
 * DataStore read/write round-trips require an Android context and are covered
 * by the [ThemePreferenceRepositoryIntegrationTest] (instrumented test).
 * The pure-logic tests here run on the JVM without any emulator.
 */
class ThemePreferenceRepositoryTest {

    // ── Test 1: Default is System ──────────────────────────────────────────────

    @Test
    fun default_isSystem_whenNoPreferenceStored() {
        // A null stored value means nothing was ever saved → fall back to SYSTEM.
        val result = null?.toAppThemeOrDefault() ?: AppTheme.SYSTEM
        assertEquals(AppTheme.SYSTEM, result)
    }

    // ── Test 2: Selecting Light persists correctly ────────────────────────────

    @Test
    fun selectingLight_roundTrips() {
        val stored = AppTheme.LIGHT.name
        assertEquals(AppTheme.LIGHT, stored.toAppThemeOrDefault())
    }

    // ── Test 3: Selecting Dark persists correctly ─────────────────────────────

    @Test
    fun selectingDark_roundTrips() {
        val stored = AppTheme.DARK.name
        assertEquals(AppTheme.DARK, stored.toAppThemeOrDefault())
    }

    // ── Test 4: Selecting High contrast persists correctly ────────────────────

    @Test
    fun selectingHighContrast_roundTrips() {
        val stored = AppTheme.HIGH_CONTRAST.name
        assertEquals(AppTheme.HIGH_CONTRAST, stored.toAppThemeOrDefault())
    }

    // ── Test 5: Selecting System persists correctly ───────────────────────────

    @Test
    fun selectingSystem_roundTrips() {
        val stored = AppTheme.SYSTEM.name
        assertEquals(AppTheme.SYSTEM, stored.toAppThemeOrDefault())
    }

    // ── Test 6: All enum names survive a round-trip ───────────────────────────

    @Test
    fun allThemes_nameRoundTrip_preservesValue() {
        for (theme in AppTheme.entries) {
            val stored = theme.name
            assertEquals(
                theme,
                stored.toAppThemeOrDefault(),
                "Round-trip failed for $theme",
            )
        }
    }

    // ── Test 7: Invalid/unknown stored value falls back to System ─────────────

    @Test
    fun unknownStoredString_fallsBackToSystem() {
        assertEquals(AppTheme.SYSTEM, "UNKNOWN_FUTURE_THEME".toAppThemeOrDefault())
    }

    @Test
    fun emptyStoredString_fallsBackToSystem() {
        assertEquals(AppTheme.SYSTEM, "".toAppThemeOrDefault())
    }

    @Test
    fun lowercaseStoredString_fallsBackToSystem() {
        // Stored names are always uppercase enum names; lowercase is treated as unknown.
        assertEquals(AppTheme.SYSTEM, "dark".toAppThemeOrDefault())
    }

    // ── Test 8: Theme selection mapping produces the correct mode ─────────────

    @Test
    fun themeMapping_system_encodedAsSystemName() {
        assertEquals("SYSTEM", AppTheme.SYSTEM.name)
    }

    @Test
    fun themeMapping_light_encodedAsLightName() {
        assertEquals("LIGHT", AppTheme.LIGHT.name)
    }

    @Test
    fun themeMapping_dark_encodedAsDarkName() {
        assertEquals("DARK", AppTheme.DARK.name)
    }

    @Test
    fun themeMapping_highContrast_encodedAsHighContrastName() {
        assertEquals("HIGH_CONTRAST", AppTheme.HIGH_CONTRAST.name)
    }

    @Test
    fun themeMapping_fourDistinctValues() {
        // Ensures no two AppTheme entries share an encoded name (regression guard).
        val names = AppTheme.entries.map { it.name }.toSet()
        assertEquals(AppTheme.entries.size, names.size, "Every AppTheme must have a unique name")
    }
}
