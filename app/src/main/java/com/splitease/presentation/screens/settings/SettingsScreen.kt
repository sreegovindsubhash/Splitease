package com.splitease.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.splitease.presentation.theme.AppTheme

/**
 * Settings screen — Appearance section.
 *
 * Stateless: [currentTheme] and [onThemeSelected] are provided by the caller.
 * The composable does not access DataStore directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics {
                            contentDescription = "Back"
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { paddingValues ->
        SettingsContent(
            currentTheme = currentTheme,
            onThemeSelected = onThemeSelected,
            paddingValues = paddingValues,
        )
    }
}

// ── Content ───────────────────────────────────────────────────────────────────

@Composable
private fun SettingsContent(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    paddingValues: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        AppearanceSection(
            currentTheme = currentTheme,
            onThemeSelected = onThemeSelected,
        )
    }
}

// ── Appearance section ────────────────────────────────────────────────────────

@Composable
private fun AppearanceSection(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
) {
    Text(
        text = "Appearance",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ThemeOptions.forEachIndexed { index, option ->
                ThemeOptionRow(
                    option = option,
                    isSelected = currentTheme == option.theme,
                    onSelect = { onThemeSelected(option.theme) },
                )
                if (index < ThemeOptions.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

// ── Individual option row ─────────────────────────────────────────────────────

private data class ThemeOption(
    val theme: AppTheme,
    val label: String,
    val description: String,
)

private val ThemeOptions = listOf(
    ThemeOption(
        theme = AppTheme.SYSTEM,
        label = "System default",
        description = "Follow the device light/dark setting",
    ),
    ThemeOption(
        theme = AppTheme.LIGHT,
        label = "Light",
        description = "Always use the light theme",
    ),
    ThemeOption(
        theme = AppTheme.DARK,
        label = "Dark",
        description = "Always use the dark theme",
    ),
    ThemeOption(
        theme = AppTheme.HIGH_CONTRAST,
        label = "High contrast",
        description = "Maximum foreground/background contrast",
    ),
)

@Composable
private fun ThemeOptionRow(
    option: ThemeOption,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    // The entire row is the touch target (≥ 48dp via minHeight padding).
    // Semantics: merge into a single TalkBack node: "Dark, selected, radio button"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = "Select ${option.label}",
                onClick = onSelect,
            )
            .semantics(mergeDescendants = true) {
                role = Role.RadioButton
                selected = isSelected
                contentDescription = buildString {
                    append(option.label)
                    if (isSelected) append(", selected")
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // RadioButton is decorative — semantics live on the row.
        RadioButton(
            selected = isSelected,
            onClick = null,   // row handles click; null avoids double TalkBack announcement
            modifier = Modifier
                .size(24.dp)
                .clearAndSetSemantics { },
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = option.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
