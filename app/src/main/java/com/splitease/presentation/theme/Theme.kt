package com.splitease.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = SplitEasePrimary,
    onPrimary = SplitEaseOnPrimary,
    primaryContainer = SplitEasePrimaryContainer,
    onPrimaryContainer = SplitEaseOnPrimaryContainer,
    secondary = SplitEaseSecondary,
    onSecondary = SplitEaseOnSecondary,
    secondaryContainer = SplitEaseSecondaryContainer,
    onSecondaryContainer = SplitEaseOnSecondaryContainer,
    tertiary = SplitEaseTertiary,
    onTertiary = SplitEaseOnTertiary,
    tertiaryContainer = SplitEaseTertiaryContainer,
    onTertiaryContainer = SplitEaseOnTertiaryContainer,
    error = SplitEaseError,
    onError = SplitEaseOnError,
    errorContainer = SplitEaseErrorContainer,
    onErrorContainer = SplitEaseOnErrorContainer,
    background = SplitEaseBackground,
    onBackground = SplitEaseOnBackground,
    surface = SplitEaseSurface,
    onSurface = SplitEaseOnSurface,
    surfaceVariant = SplitEaseSurfaceVariant,
    onSurfaceVariant = SplitEaseOnSurfaceVariant,
    outline = SplitEaseOutline,
)

private val DarkColorScheme = darkColorScheme(
    primary = SplitEasePrimaryDark,
    onPrimary = SplitEaseOnPrimaryDark,
    primaryContainer = SplitEasePrimaryContainerDark,
    onPrimaryContainer = SplitEaseOnPrimaryContainerDark,
    secondary = SplitEaseSecondaryDark,
    onSecondary = SplitEaseOnSecondaryDark,
    secondaryContainer = SplitEaseSecondaryContainerDark,
    onSecondaryContainer = SplitEaseOnSecondaryContainerDark,
    tertiary = SplitEaseTertiaryDark,
    onTertiary = SplitEaseOnTertiaryDark,
    tertiaryContainer = SplitEaseTertiaryContainerDark,
    onTertiaryContainer = SplitEaseOnTertiaryContainerDark,
    error = SplitEaseErrorDark,
    onError = SplitEaseOnErrorDark,
    errorContainer = SplitEaseErrorContainerDark,
    onErrorContainer = SplitEaseOnErrorContainerDark,
    background = SplitEaseBackgroundDark,
    onBackground = SplitEaseOnBackgroundDark,
    surface = SplitEaseSurfaceDark,
    onSurface = SplitEaseOnSurfaceDark,
    surfaceVariant = SplitEaseSurfaceVariantDark,
    onSurfaceVariant = SplitEaseOnSurfaceVariantDark,
    outline = SplitEaseOutlineDark,
)

private val HighContrastColorScheme = lightColorScheme(
    primary = HcPrimary,
    onPrimary = HcOnPrimary,
    primaryContainer = HcPrimary,
    onPrimaryContainer = HcOnPrimary,
    secondary = HcPrimary,
    onSecondary = HcOnPrimary,
    background = HcBackground,
    onBackground = HcOnBackground,
    surface = HcSurface,
    onSurface = HcOnSurface,
    error = HcError,
    onError = HcOnError,
    outline = HcOutline,
)

@Composable
fun SplitEaseTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (appTheme) {
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
        AppTheme.HIGH_CONTRAST -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when (appTheme) {
        AppTheme.HIGH_CONTRAST -> HighContrastColorScheme
        AppTheme.DARK -> DarkColorScheme
        AppTheme.LIGHT -> LightColorScheme
        AppTheme.SYSTEM -> if (darkTheme) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SplitEaseTypography,
        content = content,
    )
}
