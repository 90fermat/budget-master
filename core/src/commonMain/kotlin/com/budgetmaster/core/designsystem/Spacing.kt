package com.budgetmaster.core.designsystem

import androidx.compose.ui.unit.dp

/**
 * 8dp-grid spacing tokens (DESIGN_SYSTEM.md §4). Use these instead of raw dp literals.
 */
object Spacing {
    /** 4dp — internal badge padding, label-to-icon spacing. */
    val micro = 4.dp

    /** 8dp — list item elements, title-to-subtitle spacing. */
    val small = 8.dp

    /** 12dp — intermediate gaps inside cards. */
    val compact = 12.dp

    /** 16dp — card margins, button padding, list item gaps, mobile screen padding. */
    val medium = 16.dp

    /** 24dp — screen padding on tablet, layout breaks. */
    val large = 24.dp

    /** 32dp — top balance vertical offsets, desktop column grids. */
    val huge = 32.dp
}
