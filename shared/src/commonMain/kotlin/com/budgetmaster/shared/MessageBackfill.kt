package com.budgetmaster.shared

import androidx.compose.runtime.Composable

/**
 * Imports mobile-money messages already on the device.
 *
 * Android-only: no other platform grants an app access to SMS. Elsewhere this returns 0, which is
 * consistent with the settings section hiding itself there.
 *
 * @return how many transactions were created.
 */
@Composable
expect fun rememberMessageBackfill(): suspend () -> Int
