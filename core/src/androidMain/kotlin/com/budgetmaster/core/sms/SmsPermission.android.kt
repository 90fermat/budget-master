package com.budgetmaster.core.sms

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** Android: RECEIVE_SMS captures new messages, READ_SMS backfills what is already on the device. */
@Composable
actual fun rememberSmsPermissionRequester(
    onResult: (granted: Boolean) -> Unit,
): SmsPermissionRequester {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        // Both are needed: without RECEIVE_SMS nothing new is captured, without READ_SMS there is
        // no history — a partial grant would silently half-work.
        onResult(grants.values.all { it })
    }

    return remember(launcher) {
        object : SmsPermissionRequester {
            override val isSupported = true

            override val isGranted: Boolean
                get() = PERMISSIONS.all {
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                }

            override fun request() = launcher.launch(PERMISSIONS)
        }
    }
}

private val PERMISSIONS = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
