package com.budgetmaster.auth.presentation.biometric

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmaster.auth.domain.usecase.CheckBiometricSupportUseCase
import com.budgetmaster.auth.domain.usecase.CheckFirstLaunchUseCase
import com.budgetmaster.auth.domain.usecase.ToggleBiometricUseCase
import com.budgetmaster.auth.util.BiometricAuthenticator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Biometric setup and authentication screen.
 *
 * @param checkBiometricSupportUseCase Checks whether biometric is configured.
 * @param toggleBiometricUseCase Enables or disables biometric login.
 * @param checkFirstLaunchUseCase Checks whether onboarding is complete.
 */
class BiometricViewModel(
    private val checkBiometricSupportUseCase: CheckBiometricSupportUseCase,
    private val toggleBiometricUseCase: ToggleBiometricUseCase,
    private val checkFirstLaunchUseCase: CheckFirstLaunchUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(BiometricState())

    /** Observable UI state. */
    val state: StateFlow<BiometricState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<BiometricEffect>()

    /** Observable stream of navigation side-effects. */
    val effects: SharedFlow<BiometricEffect> = _effects.asSharedFlow()

    init {
        loadBiometricState()
    }

    private fun loadBiometricState() {
        viewModelScope.launch {
            checkBiometricSupportUseCase().collect { enabled ->
                _state.update { it.copy(isBiometricEnabled = enabled) }
            }
        }
    }

    /**
     * Processes intents from the Biometric screen.
     */
    fun onIntent(intent: BiometricIntent) {
        when (intent) {
            BiometricIntent.EnableBiometric -> {
                viewModelScope.launch {
                    try {
                        toggleBiometricUseCase(true)
                        _effects.emit(BiometricEffect.NavigateToHome)
                    } catch (e: Exception) {
                        _state.update { it.copy(errorMessage = e.message) }
                    }
                }
            }
            BiometricIntent.SkipBiometric -> {
                viewModelScope.launch { _effects.emit(BiometricEffect.NavigateToHome) }
            }
            BiometricIntent.AuthenticateWithBiometric -> {
                viewModelScope.launch { _effects.emit(BiometricEffect.NavigateToHome) }
            }
        }
    }
}
