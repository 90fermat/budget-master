package com.budgetmaster.core.backup

import androidx.compose.runtime.Composable

/**
 * Hands a finished backup to the platform: a share sheet on Android, a download on web.
 *
 * @return false when the platform could not take it, so the UI says so rather than implying the
 *   backup was saved when it was not — the one mistake that makes a backup feature worse than
 *   having none.
 */
expect suspend fun saveBackupFile(fileName: String, content: String): Boolean

/** Lets the user choose a backup file to restore from. */
interface BackupFilePicker {
    /** False where the platform offers no file picker; the restore button hides. */
    val isSupported: Boolean

    /** Opens the picker. The file's text arrives on the callback, or null if cancelled. */
    fun pick()
}

@Composable
expect fun rememberBackupFilePicker(onPicked: (String?) -> Unit): BackupFilePicker
