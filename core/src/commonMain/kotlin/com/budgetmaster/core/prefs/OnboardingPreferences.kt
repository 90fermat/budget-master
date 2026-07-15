package com.budgetmaster.core.prefs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Cross-platform persistence of whether the user has completed onboarding.
 *
 * Lives in `:core` (backed by [KeyValueStore]: DataStore on Android/iOS, `localStorage`
 * on Web) so onboarding is shown exactly once on every platform — the per-platform
 * auth repositories previously never persisted this.
 */
class OnboardingPreferences(private val store: KeyValueStore) {

    /** Emits `true` once onboarding has been completed. */
    val isCompleted: Flow<Boolean> = store.observeString(KEY).map { it == "true" }

    /** Marks onboarding as completed ([completed] = true) or resets it (false). */
    suspend fun setCompleted(completed: Boolean) {
        if (completed) store.putString(KEY, "true") else store.remove(KEY)
    }

    private companion object {
        const val KEY = "app.onboarding_completed"
    }
}
