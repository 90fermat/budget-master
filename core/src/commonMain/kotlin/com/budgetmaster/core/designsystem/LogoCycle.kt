package com.budgetmaster.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * The colours the splash mark drifts through.
 *
 * Chosen to stay legible on both the light and dark splash backgrounds, and to leave out red: on a
 * screen about someone's money red carries meaning, and it should never appear decoratively.
 *
 * Here rather than beside the splash because these are brand colours, and the architecture tests
 * are right to insist a feature not invent its own.
 */
val LogoCyclePalette: List<Color> = listOf(
    Color(0xFF34D399),
    Color(0xFF22D3EE),
    Color(0xFF818CF8),
    Color(0xFFC084FC),
    Color(0xFFFBBF24),
)

/**
 * [LogoCyclePalette] sampled at a continuous position, wrapping so the cycle has no seam.
 *
 * Takes a position rather than a time so the caller owns the animation — which keeps anything
 * drawing with it a pure function of its arguments, and therefore screenshot-testable.
 */
fun logoCycleColor(position: Float): Color {
    val size = LogoCyclePalette.size
    val wrapped = ((position % size) + size) % size
    val from = wrapped.toInt() % size
    val to = (from + 1) % size
    return lerp(LogoCyclePalette[from], LogoCyclePalette[to], wrapped - wrapped.toInt())
}
