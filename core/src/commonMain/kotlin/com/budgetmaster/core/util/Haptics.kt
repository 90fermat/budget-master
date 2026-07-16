package com.budgetmaster.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Intent-named haptics for the app's key actions.
 *
 * Deliberately no `expect/actual`: Compose Multiplatform 1.11 already exposes
 * [HapticFeedbackType] in common code with Android, iOS, and skiko backends, and it respects
 * the user's system haptic setting. Naming these by *meaning* rather than by waveform keeps
 * call sites readable and lets the mapping change centrally.
 */
@Immutable
class Haptics internal constructor(private val feedback: HapticFeedback) {

    /** A value was saved / an action succeeded. */
    fun confirm() = feedback.performHapticFeedback(HapticFeedbackType.Confirm)

    /** An action was refused: validation failed, or something is unavailable. */
    fun reject() = feedback.performHapticFeedback(HapticFeedbackType.Reject)

    /** A switch or selection flipped. */
    fun toggle(on: Boolean) = feedback.performHapticFeedback(
        if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff,
    )

    /** A destructive or long-press gesture crossed its threshold (e.g. swipe-to-delete). */
    fun longPress() = feedback.performHapticFeedback(HapticFeedbackType.LongPress)
}

/** Remembers a [Haptics] bound to the current platform feedback handler. */
@Composable
fun rememberHaptics(): Haptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) { Haptics(feedback) }
}
