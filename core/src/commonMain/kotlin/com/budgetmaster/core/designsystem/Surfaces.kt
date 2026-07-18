package com.budgetmaster.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The surface hierarchy.
 *
 * Every card previously shared one radius and no elevation story, so the balance card and a
 * budget row competed for attention and nothing read as primary. Three deliberate levels fix
 * that: exactly one [Hero] per screen, [Raised] for real content, [Flat] for supporting
 * material that should recede.
 *
 * Radius scales with importance too — a larger radius reads as a bigger, softer object, so a
 * uniform radius is what makes a screen feel like a list of equal boxes.
 */
enum class SurfaceLevel {
    /**
     * The one thing a screen is about — a balance, a net worth. At most one per screen; two
     * heroes is the same as none.
     */
    Hero,

    /** Real content: a report section, a budget card. */
    Raised,

    /** Supporting material — a note, an attribution, a hint. Recedes deliberately. */
    Flat,
}

/** Corner radius for this level. */
val SurfaceLevel.radius: Dp
    get() = when (this) {
        SurfaceLevel.Hero -> 28.dp
        SurfaceLevel.Raised -> 20.dp
        SurfaceLevel.Flat -> 14.dp
    }

/** Shape for this level. */
val SurfaceLevel.shape: Shape get() = RoundedCornerShape(radius)

/**
 * Shadow elevation.
 *
 * Kept low on purpose: heavy shadows read as dated Material rather than premium. The hierarchy is
 * carried mostly by radius, container colour and scale, with elevation only confirming it.
 */
val SurfaceLevel.elevation: Dp
    get() = when (this) {
        SurfaceLevel.Hero -> 0.dp // A hero earns prominence from colour and size, not a drop shadow.
        SurfaceLevel.Raised -> 1.dp
        SurfaceLevel.Flat -> 0.dp
    }

/** Container colour for this level. */
@Composable
fun SurfaceLevel.containerColor(): Color = when (this) {
    SurfaceLevel.Hero -> MaterialTheme.colorScheme.primaryContainer
    SurfaceLevel.Raised -> MaterialTheme.colorScheme.surface
    SurfaceLevel.Flat -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
}

/**
 * Hairline outline, or null where the container colour already separates the surface.
 *
 * A border *and* a fill *and* a shadow is the combination that reads as cheap.
 */
@Composable
fun SurfaceLevel.border(): BorderStroke? = when (this) {
    SurfaceLevel.Hero, SurfaceLevel.Flat -> null
    SurfaceLevel.Raised -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
}
