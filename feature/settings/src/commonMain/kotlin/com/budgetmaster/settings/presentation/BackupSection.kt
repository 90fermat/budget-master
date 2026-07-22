package com.budgetmaster.settings.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.action_cancel
import budgetmaster.core.generated.resources.action_continue
import budgetmaster.core.generated.resources.backup_passphrase_desc
import budgetmaster.core.generated.resources.backup_passphrase_label
import budgetmaster.core.generated.resources.backup_passphrase_restore_title
import budgetmaster.core.generated.resources.backup_passphrase_title
import budgetmaster.core.generated.resources.backup_restore_confirm_body
import budgetmaster.core.generated.resources.backup_restore_confirm_title
import budgetmaster.core.generated.resources.backup_restore_confirm_word
import budgetmaster.core.generated.resources.settings_backup_export
import budgetmaster.core.generated.resources.settings_backup_export_desc
import budgetmaster.core.generated.resources.settings_backup_import
import budgetmaster.core.generated.resources.settings_backup_import_desc
import budgetmaster.core.generated.resources.settings_backup_title
import com.budgetmaster.core.backup.BackupCrypto
import com.budgetmaster.core.backup.rememberBackupFilePicker
import com.budgetmaster.core.designsystem.Spacing
import org.jetbrains.compose.resources.stringResource

/** Which passphrase prompt is open, if any. */
private enum class PassphrasePurpose { Export, Restore }

/**
 * Backup and restore.
 *
 * Hidden where no encryption is available, rather than offering an export that would either fail
 * or — worse — produce a file the user believes is protected when it is not.
 *
 * Restore is guarded twice: the passphrase, and a typed confirmation word. It replaces everything,
 * irreversibly, so a mis-tap must not be enough to trigger it.
 */
@Composable
internal fun BackupSection(
    onExport: suspend (passphrase: String) -> String,
    onRestore: suspend (content: String, passphrase: String) -> String,
) {
    if (!BackupCrypto.isSupported) return

    var prompt by remember { mutableStateOf<PassphrasePurpose?>(null) }
    var passphrase by remember { mutableStateOf("") }
    // Held between picking the file and confirming the replacement.
    var pickedContent by remember { mutableStateOf<String?>(null) }
    var confirmText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val picker = rememberBackupFilePicker { content ->
        if (content != null) {
            pickedContent = content
            prompt = PassphrasePurpose.Restore
        }
    }

    Text(
        text = stringResource(Res.string.settings_backup_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(Spacing.small))

    BackupAction(
        enabled = !busy,
        label = stringResource(Res.string.settings_backup_export),
        description = stringResource(Res.string.settings_backup_export_desc),
        onClick = {
            passphrase = ""
            prompt = PassphrasePurpose.Export
        },
    )

    if (picker.isSupported) {
        BackupAction(
            enabled = !busy,
            label = stringResource(Res.string.settings_backup_import),
            description = stringResource(Res.string.settings_backup_import_desc),
            onClick = { picker.pick() },
        )
    }

    status?.let { message ->
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = Spacing.small),
        )
    }

    // Passphrase prompt, for either direction.
    prompt?.let { purpose ->
        val confirmWord = stringResource(Res.string.backup_restore_confirm_word)
        val restoreReady = purpose == PassphrasePurpose.Export || confirmText == confirmWord

        AlertDialog(
            onDismissRequest = {
                prompt = null
                confirmText = ""
                pickedContent = null
            },
            title = {
                Text(
                    stringResource(
                        if (purpose == PassphrasePurpose.Export) {
                            Res.string.backup_passphrase_title
                        } else {
                            Res.string.backup_passphrase_restore_title
                        },
                    ),
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(
                            if (purpose == PassphrasePurpose.Export) {
                                Res.string.backup_passphrase_desc
                            } else {
                                Res.string.backup_restore_confirm_body
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.small))
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text(stringResource(Res.string.backup_passphrase_label)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (purpose == PassphrasePurpose.Restore) {
                        Spacer(Modifier.height(Spacing.small))
                        OutlinedTextField(
                            value = confirmText,
                            onValueChange = { confirmText = it },
                            label = { Text(stringResource(Res.string.backup_restore_confirm_title)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = passphrase.isNotBlank() && restoreReady,
                    onClick = {
                        val entered = passphrase
                        val content = pickedContent
                        prompt = null
                        confirmText = ""
                        passphrase = ""
                        pickedContent = null
                        // Key stretching takes a visible moment, so this runs off the click and
                        // reports back inline rather than appearing to do nothing.
                        busy = true
                        status = null
                        scope.launch {
                            status = if (purpose == PassphrasePurpose.Export) {
                                onExport(entered)
                            } else {
                                content?.let { onRestore(it, entered) }
                            }
                            busy = false
                        }
                    },
                ) { Text(stringResource(Res.string.action_continue)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        prompt = null
                        confirmText = ""
                        pickedContent = null
                    },
                ) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun BackupAction(enabled: Boolean, label: String, description: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.small)) {
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onClick, enabled = enabled) { Text(label) }
    }
}
