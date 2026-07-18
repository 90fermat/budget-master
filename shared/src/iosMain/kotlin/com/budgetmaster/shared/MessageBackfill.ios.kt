package com.budgetmaster.shared

import androidx.compose.runtime.Composable

/** No SMS access on this platform, so there is nothing to backfill. */
@Composable
actual fun rememberMessageBackfill(): suspend () -> Int = { 0 }
