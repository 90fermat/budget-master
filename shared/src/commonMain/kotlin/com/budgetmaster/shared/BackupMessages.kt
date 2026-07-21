@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.shared

import androidx.compose.runtime.Composable
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.backup_error_not_a_backup
import budgetmaster.core.generated.resources.backup_error_version
import budgetmaster.core.generated.resources.backup_error_wrong_passphrase
import budgetmaster.core.generated.resources.backup_export_failed
import budgetmaster.core.generated.resources.backup_export_ok
import budgetmaster.core.generated.resources.backup_restore_ok
import com.budgetmaster.core.backup.BackupReadError
import com.budgetmaster.core.util.DateUtils
import com.budgetmaster.settings.domain.usecase.BackupOutcome
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

/**
 * A file name carrying the date, so a folder of backups is orderable at a glance and today's does
 * not silently overwrite last week's.
 */
fun defaultBackupFileName(): String =
    "budgetmaster-${DateUtils.isoDate(Clock.System.now().toEpochMilliseconds())}.bmbak"

/**
 * Turns a [BackupOutcome] into a sentence for the user.
 *
 * Resolved as a composable so the strings come from the current locale, then handed back as a
 * plain function the suspend callbacks can call — they run outside composition, where
 * `stringResource` is not available.
 *
 * Every failure gets its own wording. "Wrong passphrase" and "not a backup" point at completely
 * different next actions, and collapsing them into one message would leave the user retrying the
 * thing that cannot work.
 */
@Composable
fun rememberBackupMessages(): (BackupOutcome) -> String {
    val exported = stringResource(Res.string.backup_export_ok)
    val restored = stringResource(Res.string.backup_restore_ok)
    val saveFailed = stringResource(Res.string.backup_export_failed)
    val notABackup = stringResource(Res.string.backup_error_not_a_backup)
    val wrongPassphrase = stringResource(Res.string.backup_error_wrong_passphrase)
    val badVersion = stringResource(Res.string.backup_error_version)

    return { outcome ->
        when (outcome) {
            BackupOutcome.Exported -> exported
            BackupOutcome.Restored -> restored
            BackupOutcome.SaveFailed -> saveFailed
            is BackupOutcome.Unreadable -> when (outcome.reason) {
                BackupReadError.NotABackup -> notABackup
                BackupReadError.WrongPassphrase -> wrongPassphrase
                is BackupReadError.UnsupportedVersion -> badVersion
            }
        }
    }
}
