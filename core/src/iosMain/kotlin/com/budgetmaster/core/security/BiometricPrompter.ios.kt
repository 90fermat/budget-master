package com.budgetmaster.core.security

/** App lock is unsupported on this platform, so there is no prompt to show. */
actual class BiometricPrompter actual constructor() {
    actual fun isAvailable(): Boolean = false

    actual suspend fun authenticate(
        title: String,
        subtitle: String,
        cancelLabel: String,
    ): BiometricOutcome = BiometricOutcome.Unavailable
}
