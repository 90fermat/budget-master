package com.budgetmaster.auth.util

/**
 * iOS actual implementation of [BiometricAuthenticator].
 * Returns false by default – full implementation uses LocalAuthentication framework.
 */
actual class BiometricAuthenticator {
    /** Returns false – iOS biometric is not configured in this stub. */
    actual fun isAvailable(): Boolean = false
}
