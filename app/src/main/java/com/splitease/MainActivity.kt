package com.splitease

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

            SplitEaseTheme(appTheme = appTheme) {
                SplitEaseNavGraph(
                    themePreferenceRepository = app.themePreferenceRepository,
                    currentTheme = appTheme,
                )
            }
        }
    }
}
