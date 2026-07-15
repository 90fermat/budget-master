package com.budgetmaster.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

/**
 * Motion tokens (DESIGN_SYSTEM.md §6) — Material 3 Expressive curve and duration scale.
 */
object Motion {
    /** Expressive standard easing: fast start, slow settle. */
    val EasingExpressive: Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1.0f)

    /** 150ms — button states, selection clicks, micro-toggles. */
    const val DurationSnappy = 150

    /** 300ms — progress bar fills, card expands, sheet slides. */
    const val DurationMedium = 300

    /** 450ms — full-screen transitions. */
    const val DurationSlow = 450
}
