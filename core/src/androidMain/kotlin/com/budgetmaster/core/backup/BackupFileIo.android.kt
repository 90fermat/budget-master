package com.budgetmaster.core.backup

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.budgetmaster.core.db.AppContextHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Writes the backup to cache and opens the share sheet through the app's [FileProvider]. */
actual suspend fun saveBackupFile(fileName: String, content: String): Boolean =
    withContext(Dispatchers.IO) {
        val context = runCatching { AppContextHolder.context }.getOrNull() ?: return@withContext false
        runCatching {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, fileName).apply { writeText(content) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                // Deliberately not text/plain: some targets would inline a "text" share into a
                // message body and mangle it. An opaque type keeps it a file.
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        }.getOrDefault(false)
    }

/**
 * Picks a file with the Storage Access Framework and reads its text.
 *
 * `OpenDocument` rather than a permission-based file listing: it grants access to exactly the one
 * file the user chose, so restoring a backup never involves asking for storage access.
 */
@Composable
actual fun rememberBackupFilePicker(onPicked: (String?) -> Unit): BackupFilePicker {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            onPicked(null)
            return@rememberLauncherForActivityResult
        }
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        onPicked(text)
    }

    return remember(launcher) {
        object : BackupFilePicker {
            override val isSupported = true

            // Any type: a backup may have been renamed, or arrived through a service that
            // assigned it a type of its own. The file's own header decides whether it is ours.
            override fun pick() = launcher.launch(arrayOf("*/*"))
        }
    }
}
