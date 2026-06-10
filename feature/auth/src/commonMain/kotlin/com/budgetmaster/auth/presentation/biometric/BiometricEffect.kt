package com.budgetmaster.auth.presentation.biometric

sealed interface BiometricEffect {
    data object NavigateToHome : BiometricEffect
    data class ShowError(val message: String) : BiometricEffect
}
