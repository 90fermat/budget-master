package com.budgetmaster.auth.presentation.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmaster.auth.domain.model.AuthError
import com.budgetmaster.auth.domain.model.AuthException
import com.budgetmaster.auth.domain.usecase.ResetPasswordUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Forgot Password screen.
 *
 * @param resetPasswordUseCase Sends a password reset email.
 */
class ForgotPasswordViewModel(
    private val resetPasswordUseCase: ResetPasswordUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordState())

    /** Observable UI state. */
    val state: StateFlow<ForgotPasswordState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ForgotPasswordEffect>()

    /** Observable stream of navigation side-effects. */
    val effects: SharedFlow<ForgotPasswordEffect> = _effects.asSharedFlow()

    /**
     * Processes intents from the Forgot Password screen.
     */
    fun onIntent(intent: ForgotPasswordIntent) {
        when (intent) {
            is ForgotPasswordIntent.EmailChanged -> _state.update { it.copy(email = intent.email, error = null) }
            ForgotPasswordIntent.SendResetClicked -> sendReset()
            ForgotPasswordIntent.NavigateToLogin -> viewModelScope.launch { _effects.emit(ForgotPasswordEffect.NavigateToLogin) }
        }
    }

    private fun sendReset() {
        val email = _state.value.email.trim()
        if (email.isBlank()) {
            _state.update { it.copy(error = AuthError.EmptyFields) }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                resetPasswordUseCase(email)
                _state.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: AuthException) {
                _state.update { it.copy(isLoading = false, error = e.error) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = AuthError.Unknown) }
            }
        }
    }
}
