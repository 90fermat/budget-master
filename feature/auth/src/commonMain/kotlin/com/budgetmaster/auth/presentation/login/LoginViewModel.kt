package com.budgetmaster.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmaster.auth.domain.model.AuthError
import com.budgetmaster.auth.domain.model.AuthException
import com.budgetmaster.auth.domain.usecase.CheckBiometricSupportUseCase
import com.budgetmaster.auth.domain.usecase.LoginUseCase
import com.budgetmaster.auth.domain.usecase.SignInWithGoogleUseCase
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
 * @param signInWithGoogleUseCase Exchanges a Google ID token for a Firebase session.
 */
class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val checkBiometricSupportUseCase: CheckBiometricSupportUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
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
            is LoginIntent.EmailChanged -> _state.update { it.copy(email = intent.email, error = null) }
            is LoginIntent.PasswordChanged -> _state.update { it.copy(password = intent.password, error = null) }
            is LoginIntent.TogglePasswordVisibility ->
                _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            is LoginIntent.LoginClicked -> performLogin()
            is LoginIntent.BiometricLoginClicked -> handleBiometricLogin()
            is LoginIntent.GoogleSignInStarted ->
                _state.update { it.copy(isLoading = true, error = null) }
            is LoginIntent.GoogleIdTokenReceived -> performGoogleSignIn(intent.idToken)
            is LoginIntent.GoogleSignInFailed ->
                // A cancelled sheet is not an error worth shouting about.
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = intent.error.takeUnless { e -> e == AuthError.GoogleCancelled },
                    )
                }
            is LoginIntent.NavigateToRegister -> emitEffect(LoginEffect.NavigateToRegister)
            is LoginIntent.NavigateToForgotPassword -> emitEffect(LoginEffect.NavigateToForgotPassword)
        }
    }

    private fun performGoogleSignIn(idToken: String) {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                signInWithGoogleUseCase(idToken)
                emitEffect(LoginEffect.NavigateToHome)
            } catch (e: AuthException) {
                _state.update { it.copy(isLoading = false, error = e.error) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = AuthError.Unknown) }
            }
        }
    }

    private fun performLogin() {
        val current = _state.value
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                loginUseCase(current.email, current.password)
                emitEffect(LoginEffect.NavigateToHome)
            } catch (e: AuthException) {
                _state.update { it.copy(isLoading = false, error = e.error) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = AuthError.Unknown) }
            }
        }
    }

    private fun handleBiometricLogin() {
        viewModelScope.launch {
            checkBiometricSupportUseCase().collect { enabled ->
                if (enabled) {
                    emitEffect(LoginEffect.NavigateToHome)
                } else {
                    _state.update { it.copy(error = AuthError.Unknown) }
                }
            }
        }
    }

    private fun emitEffect(effect: LoginEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }
}
