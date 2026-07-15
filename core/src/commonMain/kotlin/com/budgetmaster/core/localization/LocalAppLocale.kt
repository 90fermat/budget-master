package com.budgetmaster.core.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue

/**
 * In-app locale override, following the official Compose Multiplatform
 * "resource environment" pattern.
 *
 * Wrap the app content with `CompositionLocalProvider(LocalAppLocale provides tag)`
 * and re-key the subtree (`key(tag) { … }`) so string resources reload when the
 * user changes the language in Settings. Passing `null` restores the system locale.
 */
expect object LocalAppLocale {
    /** The effective locale tag currently applied. */
    val current: String
        @Composable get

    /** Provides [value] as the app locale; `null` restores the system default. */
    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}
