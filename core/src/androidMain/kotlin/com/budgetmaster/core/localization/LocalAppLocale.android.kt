package com.budgetmaster.core.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Android implementation: swaps the JVM default locale and the active resource
 * [android.content.res.Configuration] so both Compose resources and platform
 * formatting APIs pick up the override immediately (no restart needed).
 */
actual object LocalAppLocale {
    private var default: Locale? = null

    actual val current: String
        @Composable get() = Locale.getDefault().toString()

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        if (default == null) {
            default = Locale.getDefault()
        }
        val newLocale = if (value == null) default!! else Locale.forLanguageTag(value)
        Locale.setDefault(newLocale)

        val configuration = LocalConfiguration.current
        configuration.setLocale(newLocale)
        val resources = LocalContext.current.resources
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)

        return LocalConfiguration.provides(configuration)
    }
}
