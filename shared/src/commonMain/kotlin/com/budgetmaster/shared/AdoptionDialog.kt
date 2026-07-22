package com.budgetmaster.shared

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.adoption_body
import budgetmaster.core.generated.resources.adoption_cloud
import budgetmaster.core.generated.resources.adoption_cloud_desc
import budgetmaster.core.generated.resources.adoption_device
import budgetmaster.core.generated.resources.adoption_device_desc
import budgetmaster.core.generated.resources.adoption_merge
import budgetmaster.core.generated.resources.adoption_merge_desc
import budgetmaster.core.generated.resources.adoption_title
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.sync.AdoptionChoice
import org.jetbrains.compose.resources.stringResource

/**
 * Asks what to keep when both this device and the account hold data the user made.
 *
 * Deliberately not dismissible. Every other route out of this dialog is a silent decision about
 * which of the user's own records to discard, and a stray tap on the scrim is not consent. "Keep
 * both" is listed first and described as losing nothing, because it is the answer that cannot go
 * wrong — the other two are offered for people who know they want a clean slate on one side.
 */
@Composable
internal fun AdoptionDialog(onChoice: (AdoptionChoice) -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text(stringResource(Res.string.adoption_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.adoption_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AdoptionOption(
                    label = stringResource(Res.string.adoption_merge),
                    description = stringResource(Res.string.adoption_merge_desc),
                    onClick = { onChoice(AdoptionChoice.Merge) },
                )
                Spacer(modifier = Modifier.height(Spacing.small))
                AdoptionOption(
                    label = stringResource(Res.string.adoption_device),
                    description = stringResource(Res.string.adoption_device_desc),
                    onClick = { onChoice(AdoptionChoice.ThisDeviceOnly) },
                )
                Spacer(modifier = Modifier.height(Spacing.small))
                AdoptionOption(
                    label = stringResource(Res.string.adoption_cloud),
                    description = stringResource(Res.string.adoption_cloud_desc),
                    onClick = { onChoice(AdoptionChoice.CloudOnly) },
                )
            }
        },
    )
}

@Composable
private fun AdoptionOption(label: String, description: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
