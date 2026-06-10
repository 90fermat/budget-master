package com.budgetmaster.auth.presentation.biometric

sealed interface BiometricIntent {
    data object EnableBiometric : BiometricIntent
    data object SkipBiometric : BiometricIntent
    data object AuthenticateWithBiometric : BiometricIntent
}
