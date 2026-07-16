package com.budgetmaster.auth.domain

/**
 * Whether this platform can run the Google sign-in flow.
 *
 * `true` on Android (Credential Manager). `false` on Web — the Wasm build has no Firebase
 * SDK and runs in local-only mode — and currently `false` on iOS until the Google Sign-In
 * SDK (`GIDSignIn`) is wired from Xcode. The Login screen hides the Google button wherever
 * this is `false`, so there is never a dead or throwing path.
 */
expect val isGoogleSignInSupported: Boolean
