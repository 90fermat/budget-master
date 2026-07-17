package com.budgetmaster.settings.presentation

import com.budgetmaster.core.designsystem.AppPalette
import com.budgetmaster.core.designsystem.DarkModeSetting
import com.budgetmaster.core.localization.AppLanguage

/**
 * Immutable UI state of the Settings screen.
 *
 * @property palette Currently persisted brand palette.
 * @property darkMode Currently persisted dark mode behavior.
 * @property language Currently persisted application language.
 * @property currency Currently persisted ISO currency code.
 */
data class SettingsState(
    val palette: AppPalette = AppPalette.Default,
    val darkMode: DarkModeSetting = DarkModeSetting.Default,
    val language: AppLanguage = AppLanguage.Default,
    val currency: String = "USD",
    /** Whether the user has opted in to sending spending aggregates to a cloud model. */
    val aiEnabled: Boolean = false,
)
