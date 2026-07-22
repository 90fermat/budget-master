package com.budgetmaster.core.security

/** What came back from a biometric prompt. */
enum class BiometricOutcome {
    /** The user proved who they are. */
    Success,

    /** The prompt ran and refused them — a wrong finger or face. Retryable. */
    Failed,

    /** The user dismissed it, or chose the "use PIN" fallback. Not an error. */
    Cancelled,

    /** No usable biometric hardware or enrolment, so the prompt never ran. */
    Unavailable,
}

/**
 * Shows the platform biometric prompt for the app lock.
 *
 * Lives in `:core` next to [AppLockController] because app lock is a core security concern and
 * `:core` cannot depend on a feature. It replaced a `BiometricAuthenticator` in `:feature:auth`
 * that could only report availability, never actually prompt — and whose availability check could
 * not work either, because the context it needed was never supplied to it.
 *
 * Real only on Android; the other platforms report [BiometricOutcome.Unavailable] and app lock
 * hides itself there anyway (see [isAppLockSupported]).
 */
expect class BiometricPrompter() {
    /** True when hardware exists and the user has enrolled a strong biometric. */
    fun isAvailable(): Boolean

    /**
     * Prompts, suspending until the user resolves it.
     *
     * @param cancelLabel the negative button — the PIN fallback, so it is always reachable.
     */
    suspend fun authenticate(title: String, subtitle: String, cancelLabel: String): BiometricOutcome
}
