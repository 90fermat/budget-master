package com.budgetmaster.transactions.presentation.components

import androidx.compose.ui.graphics.Color

/**
 * Parses a `#RRGGBB` (or `#AARRGGBB`) hex string into a Compose [Color],
 * falling back to [fallback] on any malformed input.
 */
internal fun parseHexColor(hex: String, fallback: Color): Color {
    val cleaned = hex.removePrefix("#")
    val value = cleaned.toLongOrNull(radix = 16) ?: return fallback
    return when (cleaned.length) {
        6 -> Color(0xFF000000 or value)
        8 -> Color(value)
        else -> fallback
    }
}
