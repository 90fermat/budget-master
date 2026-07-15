package com.budgetmaster.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** iOS has no system wallpaper-color API; always falls back to a static palette. */
@Composable
actual fun dynamicColorSchemeOrNull(darkTheme: Boolean): ColorScheme? = null
