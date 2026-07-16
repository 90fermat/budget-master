package com.budgetmaster.auth.presentation.forgotpassword

import com.budgetmaster.auth.domain.model.AuthError

data class ForgotPasswordState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: AuthError? = null,
)
