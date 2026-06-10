package com.budgetmaster.auth.presentation.forgotpassword

sealed interface ForgotPasswordIntent {
    data class EmailChanged(val email: String) : ForgotPasswordIntent
    data object SendResetClicked : ForgotPasswordIntent
    data object NavigateToLogin : ForgotPasswordIntent
}
