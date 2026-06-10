package com.budgetmaster.auth.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Onboarding screen managing page state and navigation.
 */
class OnboardingViewModel : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())

    /** Observable UI state. */
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<OnboardingEffect>()

    /** Observable stream of navigation side-effects. */
    val effects: SharedFlow<OnboardingEffect> = _effects.asSharedFlow()

    /**
     * Processes user intents from the Onboarding screen.
     */
    fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            OnboardingIntent.NextPage -> {
                val current = _state.value
                if (current.currentPage < current.totalPages - 1) {
                    _state.update { it.copy(currentPage = it.currentPage + 1) }
                } else {
                    viewModelScope.launch { _effects.emit(OnboardingEffect.NavigateToBiometric) }
                }
            }
            OnboardingIntent.PreviousPage -> {
                _state.update { it.copy(currentPage = (it.currentPage - 1).coerceAtLeast(0)) }
            }
            OnboardingIntent.Skip, OnboardingIntent.Finish -> {
                viewModelScope.launch { _effects.emit(OnboardingEffect.NavigateToBiometric) }
            }
        }
    }
}
