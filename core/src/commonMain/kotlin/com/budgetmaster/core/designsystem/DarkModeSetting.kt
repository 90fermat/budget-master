package com.budgetmaster.core.designsystem

/**
 * User-selectable dark mode behavior.
 *
 * @property id Stable identifier persisted in user preferences. Never rename.
 */
enum class DarkModeSetting(val id: String) {
    /** Follow the operating system setting. */
    SYSTEM("system"),

    /** Always light. */
    LIGHT("light"),

    /** Always dark. */
    DARK("dark");

    companion object {
        val Default = SYSTEM

        /** Resolves a persisted [id], falling back to [Default]. */
        fun fromId(id: String?): DarkModeSetting = entries.firstOrNull { it.id == id } ?: Default
    }
}
