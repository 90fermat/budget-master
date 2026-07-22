package com.budgetmaster.core.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** Android 13+ gates POST_NOTIFICATIONS at runtime; earlier versions grant it at install. */
@Composable
actual fun rememberNotificationPermissionRequester(
    onResult: (granted: Boolean) -> Unit,
): NotificationPermissionRequester {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> onResult(granted) }

    val needsRuntimeGrant = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    return remember(launcher, needsRuntimeGrant) {
        object : NotificationPermissionRequester {
            // Below Android 13 the permission is install-time, so there is nothing to prompt for
            // and isSupported is false — the caller skips the request entirely.
            override val isSupported = needsRuntimeGrant

            override val isGranted: Boolean
                get() = !needsRuntimeGrant || ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED

            override fun request() {
                if (needsRuntimeGrant) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onResult(true)
                }
            }
        }
    }
}
