package com.budgetmaster.budgets.presentation.components

import androidx.compose.ui.graphics.Color

/** Parses `#RRGGBB` / `#AARRGGBB` into a Compose [Color], falling back on bad input. */
internal fun parseHexColor(hex: String, fallback: Color): Color {
    val cleaned = hex.removePrefix("#")
    val value = cleaned.toLongOrNull(radix = 16) ?: return fallback
    return when (cleaned.length) {
        6 -> Color(0xFF000000 or value)
        8 -> Color(value)
        else -> fallback
    }
}
