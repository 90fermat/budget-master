package com.budgetmaster.core.ocr

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android picker backed by the system photo picker (`PickVisualMedia`).
 *
 * No permission is required — the user chooses one image and only that image is shared with the
 * app. Bytes are read off the main thread; a failure to read is swallowed and simply produces no
 * result, since a picker that throws on a corrupt file would be worse than one that does nothing.
 */
@Composable
actual fun rememberReceiptPicker(onImage: (ReceiptImage) -> Unit): ReceiptPicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult // cancelled
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
            }
            bytes?.let { onImage(ReceiptImage(it)) }
        }
    }

    return remember(launcher) {
        object : ReceiptPicker {
            override val isSupported = true
            override fun launch() {
                launcher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            }
        }
    }
}
