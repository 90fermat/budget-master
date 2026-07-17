package com.budgetmaster.settings.presentation

import com.budgetmaster.core.designsystem.AppPalette
import com.budgetmaster.core.designsystem.DarkModeSetting
import com.budgetmaster.core.localization.AppLanguage

/** User actions on the Settings screen. */
sealed class SettingsIntent {
    /** The user picked a brand palette. */
    data class PaletteSelected(val palette: AppPalette) : SettingsIntent()

    /** The user picked a dark mode behavior. */
    data class DarkModeSelected(val darkMode: DarkModeSetting) : SettingsIntent()

    /** The user picked an application language. */
    data class LanguageSelected(val language: AppLanguage) : SettingsIntent()

    /** The user picked a primary currency. */
    data class CurrencySelected(val currencyCode: String) : SettingsIntent()

    /** The user turned AI insights on or off. Off means nothing is sent to a cloud model. */
    data class AiEnabledChanged(val enabled: Boolean) : SettingsIntent()

    /** The user asked to replay the onboarding intro. */
    data object ReplayOnboarding : SettingsIntent()
}
