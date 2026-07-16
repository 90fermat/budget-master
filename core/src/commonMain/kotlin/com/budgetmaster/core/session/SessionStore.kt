package com.budgetmaster.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the identity of the signed-in user for the whole app.
 *
 * Lives in `:core` so feature repositories can scope their data by the current user without
 * depending on `:feature:auth`. The composition root observes the auth state and pushes the
 * uid here (and `null` on sign-out); repositories read [currentUserId] reactively.
 */
class SessionStore {

    private val _currentUserId = MutableStateFlow<String?>(null)

    /** The signed-in user's id, or `null` when nobody is authenticated. */
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    /** Sets (or clears, with `null`) the current user id. */
    fun setCurrentUser(userId: String?) {
        _currentUserId.value = userId
    }
}
