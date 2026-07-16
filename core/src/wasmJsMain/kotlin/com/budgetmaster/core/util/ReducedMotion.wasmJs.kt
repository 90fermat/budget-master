package com.budgetmaster.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window

/** Reads the CSS `prefers-reduced-motion` media query. */
@Composable
actual fun isReducedMotionEnabled(): Boolean = remember {
    runCatching { window.matchMedia("(prefers-reduced-motion: reduce)").matches }.getOrDefault(false)
}
