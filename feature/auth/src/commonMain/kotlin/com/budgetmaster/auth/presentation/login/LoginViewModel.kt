package com.budgetmaster.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmaster.auth.domain.usecase.CheckBiometricSupportUseCase
import com.budgetmaster.auth.domain.usecase.LoginUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Login screen responsible for managing state and intents.
 *
 * @param loginUseCase Validates credentials and performs authentication.
 * @param checkBiometricSupportUseCase Verifies if biometric login is configured.
 */
class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val checkBiometricSupportUseCase: CheckBiometricSupportUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())

    /** Observable state containing UI data. */
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<LoginEffect>()

    /** Observable stream of transient side-effects. */
    val effects: SharedFlow<LoginEffect> = _effects.asSharedFlow()

    /**
     * Processes incoming intents sent from the Login Composable UI.
     *
     * @param intent The user action intent to handle.
     */
    fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.EmailChanged -> _state.update { it.copy(email = intent.email, errorMessage = null) }
            is LoginIntent.PasswordChanged -> _state.update { it.copy(password = intent.password, errorMessage = null) }
            is LoginIntent.LoginClicked -> performLogin()
            is LoginIntent.BiometricLoginClicked -> handleBiometricLogin()
            is LoginIntent.NavigateToRegister -> emitEffect(LoginEffect.NavigateToRegister)
            is LoginIntent.NavigateToForgotPassword -> emitEffect(LoginEffect.NavigateToForgotPassword)
        }
    }

    private fun performLogin() {
        val current = _state.value
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                loginUseCase(current.email, current.password)
                emitEffect(LoginEffect.NavigateToHome)
            } catch (e: IllegalArgumentException) {
                _state.update { it.copy(isLoading = false, errorMessage = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, errorMessage = "Authentication failed. Please try again.") }
                emitEffect(LoginEffect.ShowError(e.message ?: "Unknown error"))
            }
        }
    }

    private fun handleBiometricLogin() {
        viewModelScope.launch {
            checkBiometricSupportUseCase().collect { enabled ->
                if (enabled) {
                    emitEffect(LoginEffect.NavigateToHome)
                } else {
                    _state.update { it.copy(errorMessage = "Biometric authentication is not available.") }
                }
            }
        }
    }

    private fun emitEffect(effect: LoginEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }
}
