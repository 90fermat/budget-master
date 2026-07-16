package com.budgetmaster.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import com.budgetmaster.core.util.isReducedMotionEnabled

/**
 * Shrinks slightly while pressed (DESIGN_SYSTEM.md §6).
 *
 * Takes the caller's [interactionSource] so the scale tracks the *same* press the clickable
 * sees — a private source would animate on the wrong gesture. No-ops under reduced motion.
 *
 * @param interactionSource the source passed to the button/clickable.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.96f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = isReducedMotionEnabled()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) pressedScale else 1f,
        animationSpec = tween(Motion.DurationSnappy, easing = Motion.EasingExpressive),
        label = "pressScale",
    )
    return this.scale(scale)
}

/**
 * Counts a monetary value up to [targetValue] (DESIGN_SYSTEM.md §6).
 *
 * Animates from the previous value on later changes, not from zero, so a balance update reads
 * as a change rather than a re-count. Under reduced motion the value snaps immediately.
 */
@Composable
fun animateCounter(targetValue: Double): State<Double> {
    val reducedMotion = isReducedMotionEnabled()
    val animatable = remember { Animatable(if (reducedMotion) targetValue.toFloat() else 0f) }

    LaunchedEffect(targetValue, reducedMotion) {
        if (reducedMotion) {
            animatable.snapTo(targetValue.toFloat())
        } else {
            animatable.animateTo(
                targetValue = targetValue.toFloat(),
                animationSpec = tween(Motion.DurationSlow, easing = Motion.EasingExpressive),
            )
        }
    }
    return remember { derivedStateOf { animatable.value.toDouble() } }
}
