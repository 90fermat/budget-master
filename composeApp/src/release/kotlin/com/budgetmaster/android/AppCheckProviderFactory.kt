package com.budgetmaster.android

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * Release builds attest with Play Integrity, which needs no per-machine setup but does require the
 * app to be a genuine install of a Play-distributed build.
 *
 * Paired with the debug source set's version of this function, so the debug-only App Check
 * artifact is never referenced from a release compile.
 */
internal fun appCheckProviderFactory(): AppCheckProviderFactory =
    PlayIntegrityAppCheckProviderFactory.getInstance()
