package com.budgetmaster.auth.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmaster.auth.domain.usecase.CompleteOnboardingUseCase
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
 *
 * Marks onboarding complete (so it is shown only once) whenever the user leaves the
 * flow — whether by finishing the last page or skipping.
 */
class OnboardingViewModel(
    private val completeOnboarding: CompleteOnboardingUseCase,
) : ViewModel() {

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
                    finishOnboarding()
                }
            }
            OnboardingIntent.PreviousPage -> {
                _state.update { it.copy(currentPage = (it.currentPage - 1).coerceAtLeast(0)) }
            }
            OnboardingIntent.Skip, OnboardingIntent.Finish -> finishOnboarding()
        }
    }

    private fun finishOnboarding() {
        viewModelScope.launch {
            completeOnboarding()
            // Always Login, never Biometric.
            //
            // This used to branch on `isBiometricAuthSupported`, which is true on Android - so a
            // first install went Splash -> Onboarding -> Biometric -> Dashboard and never asked
            // anyone to sign in. Every button on the biometric screen, including "Skip", led to
            // the Dashboard. The user landed on their finances with no session at all, which is
            // why the greeting read "Bonjour, vous".
            //
            // Biometric setup is an *enrolment* step: it protects an account, so it can only
            // sensibly run once there is an account to protect. It now lives after sign-in.
            _effects.emit(OnboardingEffect.NavigateToLogin)
        }
    }
}
