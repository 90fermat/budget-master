package com.budgetmaster.shared.lock.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.lock_biometric_cancel
import budgetmaster.core.generated.resources.lock_biometric_subtitle
import budgetmaster.core.generated.resources.lock_biometric_title
import budgetmaster.core.generated.resources.lock_locked_out
import budgetmaster.core.generated.resources.lock_subtitle
import budgetmaster.core.generated.resources.lock_title
import budgetmaster.core.generated.resources.lock_use_biometric
import budgetmaster.core.generated.resources.lock_wrong_pin
import com.budgetmaster.core.designsystem.components.PIN_LENGTH
import com.budgetmaster.core.designsystem.components.PinDots
import com.budgetmaster.core.designsystem.components.PinPad
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.security.BiometricOutcome
import com.budgetmaster.core.security.PinResult
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** How the lock screen is currently failing, if it is. */
private sealed interface LockError {
    data object WrongPin : LockError
    data class LockedOut(val seconds: Long) : LockError
}

/**
 * The unlock screen: biometrics if offered, PIN always.
 *
 * Rendered *instead of* the app, not over it, so nothing behind it is composed or readable — a
 * lock drawn on top of a live screen still leaks the balances underneath in the recents preview.
 *
 * The biometric prompt fires once on arrival when it is available, because making the user tap a
 * button to reach a fingerprint reader they already touched is friction for nothing. Cancelling it
 * falls back to the PIN rather than retrying, so the user is never trapped in a loop.
 */
@Composable
fun LockScreen(
    biometricOffered: Boolean,
    onSubmitPin: suspend (String) -> PinResult,
    onBiometric: suspend (title: String, subtitle: String, cancel: String) -> BiometricOutcome,
    onBiometricSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<LockError?>(null) }
    var busy by remember { mutableStateOf(false) }
    // Incremented to ask for the prompt: once on arrival, and again on the pad button.
    var biometricRequest by remember { mutableStateOf(0) }

    val biometricTitle = stringResource(Res.string.lock_biometric_title)
    val biometricSubtitle = stringResource(Res.string.lock_biometric_subtitle)
    val biometricCancel = stringResource(Res.string.lock_biometric_cancel)

    suspend fun runBiometric() {
        busy = true
        val outcome = onBiometric(biometricTitle, biometricSubtitle, biometricCancel)
        busy = false
        if (outcome == BiometricOutcome.Success) onBiometricSuccess()
        // Cancelled, Failed and Unavailable all just leave the PIN pad in place.
    }

    // Offer it automatically on arrival, then again whenever the pad button asks.
    LaunchedEffect(biometricOffered) {
        if (biometricOffered) biometricRequest++
    }
    LaunchedEffect(biometricRequest) {
        if (biometricRequest > 0 && biometricOffered) runBiometric()
    }

    // Tick the lockout countdown so the message stays truthful while the user waits.
    val lockedOut = error as? LockError.LockedOut
    LaunchedEffect(lockedOut) {
        var remaining = lockedOut?.seconds ?: return@LaunchedEffect
        while (remaining > 0) {
            delay(1_000)
            remaining--
            error = if (remaining > 0) LockError.LockedOut(remaining) else null
        }
    }

    suspend fun submit(candidate: String) {
        busy = true
        when (val result = onSubmitPin(candidate)) {
            is PinResult.Success -> Unit // The gate swaps this screen out.
            is PinResult.Wrong -> {
                error = LockError.WrongPin
                pin = ""
            }
            is PinResult.LockedOut -> {
                error = LockError.LockedOut(result.secondsRemaining)
                pin = ""
            }
        }
        busy = false
    }

    // Submitting on the last digit rather than behind a confirm button: the PIN length is fixed
    // per user, so an extra tap would add nothing but friction.
    LaunchedEffect(pin) {
        if (pin.length >= PIN_LENGTH) submit(pin)
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Box(Modifier.height(Spacing.medium))
            Text(
                text = stringResource(Res.string.lock_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.lock_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Box(Modifier.height(Spacing.large))
            PinDots(enteredCount = pin.length, maxLength = PIN_LENGTH)

            Box(Modifier.height(Spacing.compact))
            Text(
                text = when (val e = error) {
                    is LockError.WrongPin -> stringResource(Res.string.lock_wrong_pin)
                    is LockError.LockedOut -> stringResource(Res.string.lock_locked_out, e.seconds.toInt())
                    null -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )

            Box(Modifier.height(Spacing.large))
            PinPad(
                onDigit = { digit -> if (pin.length < PIN_LENGTH) pin += digit },
                onBackspace = { pin = pin.dropLast(1) },
                // Disabled during a lockout, so the pad itself says the wait is real.
                enabled = !busy && error !is LockError.LockedOut,
                onBiometric = if (biometricOffered) {
                    { biometricRequest++ }
                } else {
                    null
                },
                biometricIcon = {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = stringResource(Res.string.lock_use_biometric),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        }
    }

}

