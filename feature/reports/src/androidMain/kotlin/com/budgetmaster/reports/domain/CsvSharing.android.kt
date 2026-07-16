package com.budgetmaster.reports.domain

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Set once from the Android entry point. Sharing needs a Context, and this module has no
 * other route to one.
 */
object AndroidCsvSharing {
    @Volatile
    var context: Context? = null
}

/** Writes the CSV to cache and opens the system share sheet via a [FileProvider] uri. */
actual suspend fun shareCsv(fileName: String, content: String): Boolean = withContext(Dispatchers.IO) {
    val context = AndroidCsvSharing.context ?: return@withContext false
    runCatching {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName).apply { writeText(content) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    }.getOrDefault(false)
}
