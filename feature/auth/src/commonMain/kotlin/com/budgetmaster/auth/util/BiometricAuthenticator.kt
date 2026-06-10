package com.budgetmaster.auth.util

/**
 * Expect class for platform-specific biometric authentication.
 * Each platform (Android, iOS, WasmJs) provides its own actual implementation.
 */
expect class BiometricAuthenticator() {
    /**
     * Returns true if biometric hardware is available and enrolled on this device.
     */
    fun isAvailable(): Boolean
}
