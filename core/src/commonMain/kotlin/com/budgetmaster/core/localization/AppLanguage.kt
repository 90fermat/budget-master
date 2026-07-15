package com.budgetmaster.core.localization

/**
 * User-selectable application language.
 *
 * @property id Stable identifier persisted in user preferences. Never rename.
 * @property tag IETF language tag applied via [LocalAppLocale], or `null` to follow
 *   the system locale.
 */
enum class AppLanguage(val id: String, val tag: String?) {
    /** Follow the device/system locale. */
    SYSTEM("system", null),

    /** English. */
    ENGLISH("en", "en"),

    /** French. */
    FRENCH("fr", "fr");

    companion object {
        val Default = SYSTEM

        /** Resolves a persisted [id], falling back to [Default]. */
        fun fromId(id: String?): AppLanguage = entries.firstOrNull { it.id == id } ?: Default
    }
}
