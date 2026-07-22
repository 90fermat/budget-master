package com.budgetmaster.core.notifications

import androidx.compose.runtime.Composable

/**
 * Requests the OS permission needed to post system notifications.
 *
 * Only Android 13+ gates this at runtime; earlier Android auto-grants it, and the other platforms
 * do not post OS notifications from this app at all. So elsewhere [isSupported] is false and the
 * caller simply skips the request — the in-app notification inbox never needs a permission.
 *
 * Denial is not fatal: [com.budgetmaster.core.notifications] still records every notification in
 * the in-app inbox, so the information is preserved, only less immediate. That is why the result
 * gates nothing.
 */
interface NotificationPermissionRequester {
    /** False where the platform posts no OS notifications, so no request is needed. */
    val isSupported: Boolean

    /** True once the permission is held (always true below Android 13). */
    val isGranted: Boolean

    /** Prompts the user; the grant result arrives on the callback given to [rememberNotificationPermissionRequester]. */
    fun request()
}

@Composable
expect fun rememberNotificationPermissionRequester(
    onResult: (granted: Boolean) -> Unit,
): NotificationPermissionRequester
