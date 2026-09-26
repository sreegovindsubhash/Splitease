package com.splitease.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.splitease.presentation.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** DataStore delegate — one instance per process. */
private val Context.themeDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "splitease_theme_prefs")

/**
 * Persists and retrieves the user's [AppTheme] preference using DataStore Preferences.
 *
 * - Default: [AppTheme.SYSTEM]
 * - Storage key: a single string preference (the enum name).
 * - Unknown / missing values fall back to [AppTheme.SYSTEM].
 * - No Room — UI preferences do not belong in the database.
 */
class ThemePreferenceRepository(private val context: Context) {

    private val themeKey = stringPreferencesKey("app_theme")

    /** Live stream of the current theme preference. Emits immediately with the stored value. */
    val themeFlow: Flow<AppTheme> = context.themeDataStore.data
        .map { prefs ->
            val stored = prefs[themeKey]
            stored?.toAppThemeOrDefault() ?: AppTheme.SYSTEM
        }

    /** Persists [theme]. Safe to call from any coroutine — DataStore handles the IO. */
    suspend fun setTheme(theme: AppTheme) {
        context.themeDataStore.edit { prefs ->
            prefs[themeKey] = theme.name
        }
    }

    companion object {
        /**
         * Converts a stored string to [AppTheme], returning [AppTheme.SYSTEM] for any
         * unrecognised value (e.g. from a future downgrade).
         */
        internal fun String.toAppThemeOrDefault(): AppTheme =
            AppTheme.entries.firstOrNull { it.name == this } ?: AppTheme.SYSTEM
    }
}
