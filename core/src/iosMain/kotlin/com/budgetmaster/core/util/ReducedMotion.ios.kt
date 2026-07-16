package com.budgetmaster.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

/** Mirrors iOS Settings → Accessibility → Motion → Reduce Motion. */
@Composable
actual fun isReducedMotionEnabled(): Boolean = remember { UIAccessibilityIsReduceMotionEnabled() }
