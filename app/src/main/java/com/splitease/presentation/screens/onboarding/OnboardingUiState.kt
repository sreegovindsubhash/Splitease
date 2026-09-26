package com.splitease.presentation.screens.onboarding

/** UI state for the onboarding screen. */
data class OnboardingUiState(
    /** True once the user has acted (CTA or Skip) — triggers navigation. */
    val isCompleted: Boolean = false,
    /** True when the CTA was tapped (navigate to Create Group); false for Skip. */
    val navigateToCreateGroup: Boolean = false,
)
