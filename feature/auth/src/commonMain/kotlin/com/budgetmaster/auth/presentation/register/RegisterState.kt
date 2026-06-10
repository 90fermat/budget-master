package com.budgetmaster.auth.presentation.register

/**
 * UI state for the Register screen.
 */
data class RegisterState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isPasswordVisible: Boolean = false
)
