package com.budgetmaster.core.util

import androidx.compose.runtime.Composable

/**
 * Whether the user has asked the system to reduce motion.
 *
 * Animations that exist purely for delight (the splash reveal, count-ups) should be skipped
 * to their end state when this is true. Returns `false` where the platform gives us no
 * signal, so motion is only suppressed when the user actually asked for it.
 */
@Composable
expect fun isReducedMotionEnabled(): Boolean
