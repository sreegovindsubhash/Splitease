package com.splitease.presentation.theme

/**
 * Theme preference persisted by the user.
 * SYSTEM follows the device dark-mode setting.
 * HIGH_CONTRAST provides WCAG-AA contrast for accessibility.
 */
enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK,
    HIGH_CONTRAST,
}
