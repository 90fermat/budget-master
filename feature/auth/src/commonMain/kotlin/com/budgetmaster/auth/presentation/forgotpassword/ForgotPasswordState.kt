package com.budgetmaster.auth.presentation.forgotpassword

data class ForgotPasswordState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)
