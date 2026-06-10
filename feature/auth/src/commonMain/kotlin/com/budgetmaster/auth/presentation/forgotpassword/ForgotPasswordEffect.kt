package com.budgetmaster.auth.presentation.forgotpassword

sealed interface ForgotPasswordEffect {
    data object NavigateToLogin : ForgotPasswordEffect
    data class ShowMessage(val message: String) : ForgotPasswordEffect
}
