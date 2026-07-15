package com.budgetmaster.core.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

/**
 * iOS implementation: writes the `AppleLanguages` user default so the bundle
 * resource lookup uses the override, and re-provides a composition local so the
 * Compose subtree recomposes with the new locale.
 */
actual object LocalAppLocale {
    private const val LANG_KEY = "AppleLanguages"
    private val default: String = NSLocale.preferredLanguages.firstOrNull() as? String ?: "en"
    private val LocalAppLocaleState = staticCompositionLocalOf { default }

    actual val current: String
        @Composable get() = LocalAppLocaleState.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val new = value ?: default
        if (value == null) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(LANG_KEY)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(arrayListOf(new), LANG_KEY)
        }
        return LocalAppLocaleState.provides(new)
    }
}
