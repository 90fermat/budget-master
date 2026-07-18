package com.budgetmaster.core.sms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** No SMS access on this platform, so the import setting stays hidden. */
@Composable
actual fun rememberSmsPermissionRequester(
    onResult: (granted: Boolean) -> Unit,
): SmsPermissionRequester = remember {
    object : SmsPermissionRequester {
        override val isSupported = false
        override val isGranted = false
        override fun request() = Unit
    }
}
