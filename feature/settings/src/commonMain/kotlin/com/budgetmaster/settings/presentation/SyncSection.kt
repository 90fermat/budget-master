package com.budgetmaster.settings.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.sync.SyncController
import com.budgetmaster.core.sync.SyncStatus
import com.budgetmaster.core.util.DateUtils
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.settings_sync_title
import budgetmaster.core.generated.resources.settings_sync_desc
import budgetmaster.core.generated.resources.settings_sync_now
import budgetmaster.core.generated.resources.settings_sync_status_idle
import budgetmaster.core.generated.resources.settings_sync_status_syncing
import budgetmaster.core.generated.resources.settings_sync_status_synced
import budgetmaster.core.generated.resources.settings_sync_status_failed
import budgetmaster.core.generated.resources.settings_sync_status_failed_reason
import budgetmaster.core.generated.resources.settings_sync_status_signed_out
import budgetmaster.core.generated.resources.settings_sync_status_unsupported
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.collectAsState

/**
 * Sync, and what it last did.
 *
 * The status line is the whole point of the section. Sync that silently succeeds is indistinguishable
 * from sync that silently does nothing, and a user with two phones needs to be able to tell which
 * they have — particularly when the answer is "it failed", which for a phone is ordinary rather
 * than alarming and is worded that way.
 */
@Composable
internal fun SyncSection(controller: SyncController) {
    val status by controller.status.collectAsState()

    Text(
        text = stringResource(Res.string.settings_sync_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(Spacing.small))

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.small)) {
        Text(
            text = stringResource(Res.string.settings_sync_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(Spacing.small))
        Text(
            text = status.describe(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(Spacing.medium))
        Button(
            onClick = { controller.requestSync() },
            enabled = status !is SyncStatus.Syncing && status != SyncStatus.Unsupported,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.settings_sync_now))
        }
    }
}

@Composable
private fun SyncStatus.describe(): String = when (this) {
    SyncStatus.Idle -> stringResource(Res.string.settings_sync_status_idle)
    SyncStatus.Syncing -> stringResource(Res.string.settings_sync_status_syncing)
    is SyncStatus.Synced -> stringResource(Res.string.settings_sync_status_synced, DateUtils.isoDate(at))
    // Deliberately not phrased as an error. A phone without signal is the normal case, the data is
    // safe locally, and the next pass resumes where this one stopped — so the message says so
    // rather than inviting the user to worry or to retry pointlessly.
    // The reason is shown when there is one. Without it the message was the same sentence for a
    // missing network, a rejected write and a timeout — which is exactly the information needed to
    // tell those apart, discarded at the point it mattered.
    is SyncStatus.Failed -> reason
        ?.let { stringResource(Res.string.settings_sync_status_failed_reason, it) }
        ?: stringResource(Res.string.settings_sync_status_failed)
    SyncStatus.SignedOut -> stringResource(Res.string.settings_sync_status_signed_out)
    SyncStatus.Unsupported -> stringResource(Res.string.settings_sync_status_unsupported)
}
