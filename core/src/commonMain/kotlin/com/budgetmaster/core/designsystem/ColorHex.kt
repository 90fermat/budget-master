package com.budgetmaster.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Parses a `#RRGGBB` or `#AARRGGBB` hex string into a [Color].
 *
 * Category accent colors are stored as hex in the database, so this turns *data* into a
 * color — it is not a hardcoded palette. It lives in the design system because three
 * features needed it and each had grown its own copy.
 *
 * @param fallback returned for any malformed input, so bad stored data can't crash the UI.
 */
fun parseHexColor(hex: String, fallback: Color): Color {
    val cleaned = hex.removePrefix("#")
    val value = cleaned.toLongOrNull(radix = 16) ?: return fallback
    return when (cleaned.length) {
        6 -> Color(0xFF000000 or value)
        8 -> Color(value)
        else -> fallback
    }
}
