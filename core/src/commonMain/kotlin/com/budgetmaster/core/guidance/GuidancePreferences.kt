package com.budgetmaster.core.guidance

import com.budgetmaster.core.prefs.KeyValueStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Remembers which screen guides have been shown, and whether to show them at all.
 *
 * Mirrors `OnboardingPreferences`: durable via [KeyValueStore] (DataStore on Android/iOS,
 * `localStorage` on Web), so a guide shown once stays shown — the point is to explain, not to
 * nag.
 */
class GuidancePreferences(private val store: KeyValueStore) {

    /** Whether to auto-open a guide on a screen's first visit. Defaults to on. */
    val tipsEnabled: Flow<Boolean> =
        store.observeString(KEY_TIPS_ENABLED).map { it == null || it.toBoolean() }

    /** Whether [key]'s guide has already been shown. */
    fun hasSeen(key: GuidanceKey): Flow<Boolean> =
        store.observeString(seenKey(key)).map { it.toBoolean() }

    /** Records that [key]'s guide has been shown, so it won't auto-open again. */
    suspend fun markSeen(key: GuidanceKey) {
        store.putString(seenKey(key), true.toString())
    }

    /** Turns the auto-opening guides on or off. The `?` button always works regardless. */
    suspend fun setTipsEnabled(enabled: Boolean) {
        store.putString(KEY_TIPS_ENABLED, enabled.toString())
    }

    /** Forgets every guide, so each screen explains itself once more. */
    suspend fun resetAll() {
        GuidanceKey.entries.forEach { store.remove(seenKey(it)) }
    }

    private fun seenKey(key: GuidanceKey) = "$KEY_SEEN_PREFIX${key.name}"

    private companion object {
        const val KEY_TIPS_ENABLED = "guidance.tips_enabled"
        const val KEY_SEEN_PREFIX = "guidance.seen."
    }
}
