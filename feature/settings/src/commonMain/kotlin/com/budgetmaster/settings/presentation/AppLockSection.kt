@file:OptIn(ExperimentalMaterial3Api::class)

package com.budgetmaster.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.pin_setup_title
import budgetmaster.core.generated.resources.settings_lock_biometric
import budgetmaster.core.generated.resources.settings_lock_biometric_desc
import budgetmaster.core.generated.resources.settings_lock_change_pin
import budgetmaster.core.generated.resources.settings_lock_enable
import budgetmaster.core.generated.resources.settings_lock_enable_desc
import budgetmaster.core.generated.resources.settings_lock_set_pin
import budgetmaster.core.generated.resources.settings_lock_timeout
import budgetmaster.core.generated.resources.settings_lock_timeout_immediate
import budgetmaster.core.generated.resources.settings_lock_timeout_minutes
import budgetmaster.core.generated.resources.settings_lock_timeout_seconds
import budgetmaster.core.generated.resources.settings_lock_title
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.designsystem.components.PinEntryDialog
import com.budgetmaster.core.security.isAppLockSupported
import org.jetbrains.compose.resources.stringResource

/** Why the PIN dialog is open, so completing it does the right follow-up. */
private enum class PinPurpose { SetThenEnable, Change }

/**
 * App lock: require biometrics or a PIN to open the app.
 *
 * Hidden entirely where the platform cannot support it, rather than offering a switch that
 * protects nothing — the same rule the SMS import section follows.
 *
 * Turning the lock on asks for a PIN first when none exists, and the switch only follows once one
 * is set. Biometrics can be un-enrolled at any time, so a lock with no PIN behind it is a lock
 * that can strand its owner outside their own ledger.
 */
@Composable
internal fun AppLockSection(
    enabled: Boolean,
    pinSet: Boolean,
    biometricEnabled: Boolean,
    timeoutSeconds: Int,
    onEnabledChange: (Boolean) -> Unit,
    onPinChosen: (String) -> Unit,
    onBiometricChange: (Boolean) -> Unit,
    onTimeoutChange: (Int) -> Unit,
) {
    if (!isAppLockSupported) return

    var pinPrompt by remember { mutableStateOf<PinPurpose?>(null) }

    Text(
        text = stringResource(Res.string.settings_lock_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(Spacing.small))

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.settings_lock_enable),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(Res.string.settings_lock_enable_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(Spacing.small))
        Switch(
            checked = enabled,
            onCheckedChange = { wants ->
                if (wants && !pinSet) {
                    pinPrompt = PinPurpose.SetThenEnable
                } else {
                    onEnabledChange(wants)
                }
            },
        )
    }

    if (enabled) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.settings_lock_biometric),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(Res.string.settings_lock_biometric_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(Spacing.small))
            Switch(checked = biometricEnabled, onCheckedChange = onBiometricChange)
        }

        LockTimeoutPicker(timeoutSeconds = timeoutSeconds, onTimeoutChange = onTimeoutChange)

        TextButton(onClick = { pinPrompt = PinPurpose.Change }) {
            Text(
                stringResource(
                    if (pinSet) Res.string.settings_lock_change_pin else Res.string.settings_lock_set_pin,
                ),
            )
        }
    }

    pinPrompt?.let { purpose ->
        PinEntryDialog(
            title = stringResource(Res.string.pin_setup_title),
            confirm = true,
            onComplete = { pin ->
                onPinChosen(pin)
                if (purpose == PinPurpose.SetThenEnable) onEnabledChange(true)
                pinPrompt = null
            },
            onDismiss = { pinPrompt = null },
        )
    }
}

/** How long the app may sit in the background before it re-locks. */
@Composable
private fun LockTimeoutPicker(timeoutSeconds: Int, onTimeoutChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(0, 30, 60, 300)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.padding(vertical = Spacing.small),
    ) {
        OutlinedTextField(
            value = timeoutLabel(timeoutSeconds),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.settings_lock_timeout)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { seconds ->
                DropdownMenuItem(
                    text = { Text(timeoutLabel(seconds)) },
                    onClick = {
                        onTimeoutChange(seconds)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun timeoutLabel(seconds: Int): String = when {
    seconds == 0 -> stringResource(Res.string.settings_lock_timeout_immediate)
    seconds < 60 -> stringResource(Res.string.settings_lock_timeout_seconds, seconds)
    else -> stringResource(Res.string.settings_lock_timeout_minutes, seconds / 60)
}
