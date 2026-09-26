package com.splitease.presentation.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.splitease.data.repository.OnboardingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val onboardingPreferenceRepository: OnboardingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /** Called when the user taps "Create your first group". */
    fun onCreateFirstGroup() {
        viewModelScope.launch {
            onboardingPreferenceRepository.setOnboardingCompleted()
            _uiState.update { it.copy(isCompleted = true, navigateToCreateGroup = true) }
        }
    }

    /** Called when the user taps "Skip". */
    fun onSkip() {
        viewModelScope.launch {
            onboardingPreferenceRepository.setOnboardingCompleted()
            _uiState.update { it.copy(isCompleted = true, navigateToCreateGroup = false) }
        }
    }

    /** Call after the navigation side-effect has been consumed. */
    fun onNavigationConsumed() {
        _uiState.update { it.copy(isCompleted = false, navigateToCreateGroup = false) }
    }
}
