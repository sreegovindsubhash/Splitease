package com.splitease

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.splitease.presentation.navigation.Screen
import com.splitease.presentation.navigation.SplitEaseNavGraph
import com.splitease.presentation.theme.AppTheme
import com.splitease.presentation.theme.SplitEaseTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SplitEaseApplication

        setContent {
            // Observe the persisted theme preference reactively.
            // Starts as SYSTEM so the first frame is always valid.
            val appTheme: AppTheme by app.themePreferenceRepository.themeFlow
                .collectAsStateWithLifecycle(initialValue = AppTheme.SYSTEM)

            // Observe onboarding completion. Use null as the initial value so we
            // don't render the NavGraph until the flag has been read from DataStore,
            // avoiding a flash to the wrong destination.
            val onboardingCompleted: Boolean? by app.onboardingPreferenceRepository
                .isOnboardingCompleted
                .collectAsStateWithLifecycle(initialValue = null)

            SplitEaseTheme(appTheme = appTheme) {
                // Wait for the onboarding flag before composing the graph.
                val completed = onboardingCompleted ?: return@SplitEaseTheme
                val startDestination = if (completed) {
                    Screen.Groups.route
                } else {
                    Screen.Onboarding.route
                }
                SplitEaseNavGraph(
                    startDestination = startDestination,
                    themePreferenceRepository = app.themePreferenceRepository,
                    currentTheme = appTheme,
                )
            }
        }
    }
}
