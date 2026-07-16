package com.budgetmaster.auth.presentation.register

import com.budgetmaster.auth.domain.model.AuthError

/**
 * UI state for the Register screen.
 */
data class RegisterState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val isPasswordVisible: Boolean = false,
)
