package com.budgetmaster.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * Returns a Material You (wallpaper-derived) color scheme when the platform supports it
 * — Android 12+ — or `null` otherwise, so callers can fall back to a static palette.
 */
@Composable
expect fun dynamicColorSchemeOrNull(darkTheme: Boolean): ColorScheme?
