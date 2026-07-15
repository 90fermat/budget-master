package com.budgetmaster.core.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.intl.Locale

/**
 * Wasm implementation: sets `window.__customLocale`, which `index.html` exposes as
 * the first entry of `navigator.languages` (see the shim script there), so
 * Compose resources resolve the overridden locale.
 */
actual object LocalAppLocale {
    private val LocalAppLocaleState = staticCompositionLocalOf { Locale.current.toString() }

    actual val current: String
        @Composable get() = LocalAppLocaleState.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        setCustomLocale(value?.replace('_', '-'))
        return LocalAppLocaleState.provides(value ?: Locale.current.toString())
    }
}

private fun setCustomLocale(value: String?): Unit = js("{ window.__customLocale = value; }")
