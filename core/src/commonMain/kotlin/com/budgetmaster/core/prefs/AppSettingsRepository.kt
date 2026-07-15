package com.budgetmaster.core.prefs

import com.budgetmaster.core.designsystem.AppPalette
import com.budgetmaster.core.designsystem.DarkModeSetting
import com.budgetmaster.core.localization.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * App-wide user settings applied by the app shell in `:shared`.
 *
 * @property palette Selected brand palette.
 * @property darkMode Dark mode behavior.
 * @property language Selected application language.
 */
data class AppSettings(
    val palette: AppPalette = AppPalette.Default,
    val darkMode: DarkModeSetting = DarkModeSetting.Default,
    val language: AppLanguage = AppLanguage.Default,
)

/**
 * Persists and observes [AppSettings] through the platform [KeyValueStore].
 *
 * Lives in `:core` (not a feature) because the theme and locale must be applied by
 * the `:shared` app shell before any feature renders; `:feature:settings` mutates
 * these values through its use cases.
 */
class AppSettingsRepository(private val store: KeyValueStore) {

    /** Reactive stream of the current settings; emits defaults until first write. */
    val settings: Flow<AppSettings> = combine(
        store.observeString(KEY_PALETTE),
        store.observeString(KEY_DARK_MODE),
        store.observeString(KEY_LANGUAGE),
    ) { palette, darkMode, language ->
        AppSettings(
            palette = AppPalette.fromId(palette),
            darkMode = DarkModeSetting.fromId(darkMode),
            language = AppLanguage.fromId(language),
        )
    }

    suspend fun setPalette(palette: AppPalette) = store.putString(KEY_PALETTE, palette.id)

    suspend fun setDarkMode(darkMode: DarkModeSetting) = store.putString(KEY_DARK_MODE, darkMode.id)

    suspend fun setLanguage(language: AppLanguage) = store.putString(KEY_LANGUAGE, language.id)

    private companion object {
        const val KEY_PALETTE = "app.palette"
        const val KEY_DARK_MODE = "app.dark_mode"
        const val KEY_LANGUAGE = "app.language"
    }
}
