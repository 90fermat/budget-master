package com.budgetmaster.settings.domain.usecase

import com.budgetmaster.core.backup.BackupException
import com.budgetmaster.core.backup.BackupFile
import com.budgetmaster.core.backup.BackupReadError
import com.budgetmaster.core.backup.BackupService
import com.budgetmaster.core.backup.saveBackupFile

/** What happened, so the UI can say something specific rather than "it failed". */
sealed interface BackupOutcome {
    data object Exported : BackupOutcome
    data object Restored : BackupOutcome
    data object SaveFailed : BackupOutcome
    data class Unreadable(val reason: BackupReadError) : BackupOutcome
}

/**
 * Exports the database to an encrypted file and hands it to the platform.
 *
 * The passphrase is used and dropped; nothing about it is retained, so a backup cannot be opened
 * from this device any more easily than from anywhere else.
 */
class ExportBackupUseCase(private val service: BackupService) {
    suspend operator fun invoke(passphrase: String, fileName: String): BackupOutcome {
        val content = BackupFile.write(service.export(), passphrase)
        return if (saveBackupFile(fileName, content)) BackupOutcome.Exported else BackupOutcome.SaveFailed
    }
}

/**
 * Restores from a backup file, replacing everything.
 *
 * Decrypts and parses **before** touching the database, so an unreadable file or a wrong
 * passphrase leaves the existing data exactly as it was. Only a fully-parsed envelope gets as far
 * as the replace.
 */
class RestoreBackupUseCase(private val service: BackupService) {
    suspend operator fun invoke(content: String, passphrase: String): BackupOutcome {
        val envelope = BackupFile.read(content, passphrase).getOrElse { error ->
            val reason = (error as? BackupException)?.error ?: BackupReadError.NotABackup
            return BackupOutcome.Unreadable(reason)
        }
        service.restore(envelope)
        return BackupOutcome.Restored
    }
}
