package com.budgetmaster.core.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** No OS notifications are posted on this platform, so there is nothing to request. */
@Composable
actual fun rememberNotificationPermissionRequester(
    onResult: (granted: Boolean) -> Unit,
): NotificationPermissionRequester = remember {
    object : NotificationPermissionRequester {
        override val isSupported = false
        override val isGranted = false
        override fun request() = onResult(false)
    }
}
