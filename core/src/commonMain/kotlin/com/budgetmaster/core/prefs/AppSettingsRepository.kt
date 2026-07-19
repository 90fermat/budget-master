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
 * @property currency ISO-4217 currency code used to format amounts app-wide.
 * @property aiEnabled Whether AI features may send anything to a cloud model.
 */
data class AppSettings(
    val palette: AppPalette = AppPalette.Default,
    val darkMode: DarkModeSetting = DarkModeSetting.Default,
    val language: AppLanguage = AppLanguage.Default,
    val currency: String = "USD",
    /**
     * **Off by default, and deliberately so.** Turning this on lets the app send a summary of
     * the user's spending to a third-party model. Nobody's financial data should leave their
     * device because they never found a setting, so this is opt-in rather than opt-out.
     */
    val aiEnabled: Boolean = false,
    /**
     * Whether incoming mobile-money messages are read and imported automatically.
     *
     * Off by default and gated on the SMS permission: reading someone's messages is the most
     * invasive thing this app can do, so it happens only after an explicit opt-in.
     */
    val smsImportEnabled: Boolean = false,
    /**
     * The user's own mobile-money numbers, comma-separated.
     *
     * Required for import to work at all: "Transfert de A vers B" is the same sentence whether
     * money arrived or left, and only matching these numbers against each side resolves it.
     */
    val smsOwnerMsisdns: String = "",
    /**
     * Whether to block screenshots and hide the app's contents from the recents screen.
     *
     * **On by default**, unlike the other privacy switches here. Those govern what leaves the
     * device, where an unset preference must never be read as consent. This one governs what a
     * passer-by can see over the user's shoulder or in a recents thumbnail, and defaulting it off
     * would mean shipping balances visible in the app switcher until someone thought to look for
     * a setting. It is a toggle rather than fixed because it also blocks the user's own
     * screenshots, which is occasionally a legitimate thing to want.
     */
    val secureScreen: Boolean = true,
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
        store.observeString(KEY_CURRENCY),
        store.observeString(KEY_AI_ENABLED),
        store.observeString(KEY_SMS_IMPORT_ENABLED),
        store.observeString(KEY_SMS_OWNER_MSISDNS),
        store.observeString(KEY_SECURE_SCREEN),
    ) { values ->
        AppSettings(
            palette = AppPalette.fromId(values[0]),
            darkMode = DarkModeSetting.fromId(values[1]),
            language = AppLanguage.fromId(values[2]),
            currency = values[3] ?: "USD",
            // Absent means off: an unset preference must never be read as consent.
            aiEnabled = values[4].toBoolean(),
            smsImportEnabled = values[5].toBoolean(),
            smsOwnerMsisdns = values[6].orEmpty(),
            // Absent means on: see the property doc for why this default runs the other way.
            secureScreen = values[7]?.toBoolean() ?: true,
        )
    }

    suspend fun setPalette(palette: AppPalette) = store.putString(KEY_PALETTE, palette.id)

    suspend fun setDarkMode(darkMode: DarkModeSetting) = store.putString(KEY_DARK_MODE, darkMode.id)

    suspend fun setLanguage(language: AppLanguage) = store.putString(KEY_LANGUAGE, language.id)

    suspend fun setCurrency(currencyCode: String) = store.putString(KEY_CURRENCY, currencyCode)

    suspend fun setAiEnabled(enabled: Boolean) = store.putString(KEY_AI_ENABLED, enabled.toString())

    suspend fun setSmsImportEnabled(enabled: Boolean) =
        store.putString(KEY_SMS_IMPORT_ENABLED, enabled.toString())

    suspend fun setSecureScreen(enabled: Boolean) =
        store.putString(KEY_SECURE_SCREEN, enabled.toString())

    /** @param msisdns comma-separated; whitespace is tolerated and stripped on read. */
    suspend fun setSmsOwnerMsisdns(msisdns: String) =
        store.putString(KEY_SMS_OWNER_MSISDNS, msisdns)

    // Public rather than private so tests in other modules can seed a preference before the
    // subject first reads it, instead of hardcoding the key string.
    companion object {
        const val KEY_PALETTE = "app.palette"
        const val KEY_DARK_MODE = "app.dark_mode"
        const val KEY_LANGUAGE = "app.language"
        const val KEY_CURRENCY = "app.currency"
        const val KEY_AI_ENABLED = "app.ai_enabled"
        const val KEY_SECURE_SCREEN = "app.secure_screen"
        const val KEY_SMS_IMPORT_ENABLED = "app.sms_import_enabled"
        const val KEY_SMS_OWNER_MSISDNS = "app.sms_owner_msisdns"
    }
}
