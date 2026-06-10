package com.budgetmaster.auth.presentation.biometric

data class BiometricState(
    val isBiometricEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
