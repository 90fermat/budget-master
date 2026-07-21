package com.budgetmaster.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.action_cancel
import budgetmaster.core.generated.resources.pin_confirm_mismatch
import budgetmaster.core.generated.resources.pin_confirm_title
import budgetmaster.core.generated.resources.pin_setup_subtitle
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.designsystem.SurfaceLevel
import com.budgetmaster.core.designsystem.shape
import org.jetbrains.compose.resources.stringResource

/**
 * Asks for a PIN once, or twice when [confirm] is set, and hands back the digits.
 *
 * Used for setting a new PIN (enter then confirm) and for proving the current one before a change.
 * The PIN never leaves this composable except through [onComplete]; the caller hashes it.
 */
@Composable
fun PinEntryDialog(
    title: String,
    confirm: Boolean,
    onComplete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var first by remember { mutableStateOf<String?>(null) }
    var pin by remember { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }

    fun accept(entered: String) {
        when {
            !confirm -> onComplete(entered)
            first == null -> {
                first = entered
                pin = ""
            }
            first == entered -> onComplete(entered)
            else -> {
                // Start over rather than let a typo become a PIN the user does not know.
                first = null
                pin = ""
                mismatch = true
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = SurfaceLevel.Hero.shape,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(Spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (confirm && first != null) {
                        stringResource(Res.string.pin_confirm_title)
                    } else {
                        title
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(Res.string.pin_setup_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Box(Modifier.height(Spacing.medium))
                PinDots(enteredCount = pin.length, maxLength = PIN_LENGTH)

                if (mismatch) {
                    Box(Modifier.height(Spacing.small))
                    Text(
                        text = stringResource(Res.string.pin_confirm_mismatch),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }

                Box(Modifier.height(Spacing.medium))
                PinPad(
                    onDigit = { digit ->
                        mismatch = false
                        if (pin.length < PIN_LENGTH) {
                            pin += digit
                            if (pin.length == PIN_LENGTH) accept(pin)
                        }
                    },
                    onBackspace = { pin = pin.dropLast(1) },
                    enabled = true,
                )

                Box(Modifier.height(Spacing.small))
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
            }
        }
    }
}
