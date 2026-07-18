package com.budgetmaster.core.sms

import androidx.compose.runtime.Composable

/**
 * Requests the platform permission needed to read mobile-money messages.
 *
 * Only Android can do this at all — iOS gives no app access to SMS, by design — so elsewhere
 * [isSupported] is false and the setting hides itself rather than offering something that can
 * never work.
 */
interface SmsPermissionRequester {
    /** False where the platform has no SMS access to grant. */
    val isSupported: Boolean

    /** True once the permissions are held. */
    val isGranted: Boolean

    /** Prompts the user. The result arrives on the callback given to [rememberSmsPermissionRequester]. */
    fun request()
}

@Composable
expect fun rememberSmsPermissionRequester(onResult: (granted: Boolean) -> Unit): SmsPermissionRequester
