package com.budgetmaster.core.backup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// Backup reports itself unsupported on this platform (BackupCrypto.isSupported is false), so the
// UI never reaches these. Present so the shared code compiles across targets.
actual suspend fun saveBackupFile(fileName: String, content: String): Boolean = false

@Composable
actual fun rememberBackupFilePicker(onPicked: (String?) -> Unit): BackupFilePicker = remember {
    object : BackupFilePicker {
        override val isSupported = false
        override fun pick() = onPicked(null)
    }
}
