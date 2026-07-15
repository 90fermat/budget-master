package com.budgetmaster.auth.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmaster.auth.domain.model.AuthStatus
import com.budgetmaster.auth.domain.usecase.CheckAuthStatusUseCase
import com.budgetmaster.auth.domain.usecase.CheckFirstLaunchUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Splash screen that determines the initial navigation destination.
 *
 * @param checkAuthStatusUseCase Checks if a user is authenticated.
 * @param checkFirstLaunchUseCase Checks if the onboarding was already completed.
 */
class SplashViewModel(
    private val checkAuthStatusUseCase: CheckAuthStatusUseCase,
    private val checkFirstLaunchUseCase: CheckFirstLaunchUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SplashState())

    /** Observable UI state. */
    val state: StateFlow<SplashState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<SplashEffect>(replay = 1)

    /** Observable stream of navigation side-effects. */
    val effects: SharedFlow<SplashEffect> = _effects.asSharedFlow()

    init {
        resolveDestination()
    }

    private fun resolveDestination() {
        viewModelScope.launch {
            combine(
                checkAuthStatusUseCase(),
                checkFirstLaunchUseCase()
            ) { authStatus, onboardingCompleted ->
                Pair(authStatus, onboardingCompleted)
            }.collect { (authStatus, onboardingCompleted) ->
                _state.update { it.copy(isLoading = false) }
                val effect = when {
                    authStatus is AuthStatus.Authenticated -> SplashEffect.NavigateToDashboard
                    !onboardingCompleted -> SplashEffect.NavigateToOnboarding
                    else -> SplashEffect.NavigateToLogin
                }
                _effects.emit(effect)
            }
        }
    }
}
