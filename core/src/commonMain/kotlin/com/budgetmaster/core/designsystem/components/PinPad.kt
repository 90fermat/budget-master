package com.budgetmaster.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.lock_delete_digit
import com.budgetmaster.core.designsystem.Spacing
import org.jetbrains.compose.resources.stringResource

/**
 * A fixed PIN length, so entry can submit itself on the last digit and the dots show progress
 * honestly. Six rather than four: the keyspace is still small, but 100x costs the user nothing.
 */
const val PIN_LENGTH = 6

/**
 * The filled/empty dots showing how many digits have been entered.
 *
 * Deliberately does not show the digits themselves, or their count as a number — a shoulder-surfer
 * learning the PIN's length is a real, cheap win for them.
 */
@Composable
fun PinDots(
    enteredCount: Int,
    maxLength: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.compact),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(maxLength) { index ->
            val filled = index < enteredCount
            Box(
                modifier = Modifier
                    .size(if (filled) 14.dp else 10.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (filled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    },
                    modifier = Modifier.size(if (filled) 14.dp else 10.dp),
                ) {}
            }
        }
    }
}

/**
 * A numeric keypad.
 *
 * Its own component rather than a system keyboard: a PIN entry that raises the IME lets the
 * platform's text prediction and clipboard see the digits, and a lock screen should not hand the
 * user's PIN to anything it does not have to.
 *
 * @param onBiometric shown in the empty bottom-left slot when biometrics are offered.
 */
@Composable
fun PinPad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onBiometric: (() -> Unit)? = null,
    biometricIcon: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.compact),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        listOf("123", "456", "789").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.large)) {
                row.forEach { digit ->
                    PinKey(enabled = enabled, onClick = { onDigit(digit) }) {
                        Text(
                            text = digit.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.large)) {
            // Bottom-left: biometrics when available, otherwise an empty slot so the "0" key
            // stays in the middle where the eye expects it.
            if (onBiometric != null) {
                PinKey(enabled = enabled, onClick = onBiometric, content = biometricIcon)
            } else {
                Box(Modifier.size(KEY_SIZE))
            }
            PinKey(enabled = enabled, onClick = { onDigit('0') }) {
                Text(
                    text = "0",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                )
            }
            PinKey(enabled = enabled, onClick = onBackspace) {
                Icon(
                    Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = stringResource(Res.string.lock_delete_digit),
                )
            }
        }
    }
}

private val KEY_SIZE = 72.dp

@Composable
private fun PinKey(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.size(KEY_SIZE),
    ) {
        Box(Modifier.padding(Spacing.small), contentAlignment = Alignment.Center) { content() }
    }
}
