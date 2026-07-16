package com.budgetmaster.auth.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmaster.auth.domain.model.AuthError
import com.budgetmaster.auth.domain.model.AuthException
import com.budgetmaster.auth.domain.usecase.SignUpUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Register screen.
 *
 * @param signUpUseCase Use case to register a new user.
 */
class RegisterViewModel(
    private val signUpUseCase: SignUpUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())

    /** Observable UI state. */
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<RegisterEffect>()

    /** Observable stream of navigation side-effects. */
    val effects: SharedFlow<RegisterEffect> = _effects.asSharedFlow()

    /**
     * Processes intents from the Register screen.
     */
    fun onIntent(intent: RegisterIntent) {
        when (intent) {
            is RegisterIntent.EmailChanged -> _state.update { it.copy(email = intent.email, error = null) }
            is RegisterIntent.PasswordChanged -> _state.update { it.copy(password = intent.password, error = null) }
            is RegisterIntent.ConfirmPasswordChanged ->
                _state.update { it.copy(confirmPassword = intent.confirmPassword, error = null) }
            is RegisterIntent.TogglePasswordVisibility ->
                _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            RegisterIntent.RegisterClicked -> performRegistration()
            RegisterIntent.NavigateToLogin -> viewModelScope.launch { _effects.emit(RegisterEffect.NavigateToLogin) }
        }
    }

    private fun performRegistration() {
        val current = _state.value
        if (current.password != current.confirmPassword) {
            _state.update { it.copy(error = AuthError.PasswordMismatch) }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                signUpUseCase(current.email.trim(), current.password)
                _effects.emit(RegisterEffect.NavigateToHome)
            } catch (e: AuthException) {
                _state.update { it.copy(isLoading = false, error = e.error) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = AuthError.Unknown) }
            }
        }
    }
}
