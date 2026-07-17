package com.budgetmaster.android

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds attest with a debug token rather than Play Integrity, which only works for real
 * installs from Play.
 *
 * The token is printed to logcat on first launch and must be registered once per machine or
 * emulator under **App Check → Apps → Manage debug tokens** in the Firebase console. Until it is,
 * Firebase AI Logic rejects the insights call.
 *
 * This lives in the debug source set because `firebase-appcheck-debug` is a `debugImplementation`
 * dependency: referencing it from `src/main` compiles in debug and then fails the release build.
 */
internal fun appCheckProviderFactory(): AppCheckProviderFactory =
    DebugAppCheckProviderFactory.getInstance()
