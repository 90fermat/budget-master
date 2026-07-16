package com.budgetmaster.auth.domain

/** The Web build runs local-only auth (no Firebase SDK for Wasm), so Google sign-in is off. */
actual val isGoogleSignInSupported: Boolean = false
