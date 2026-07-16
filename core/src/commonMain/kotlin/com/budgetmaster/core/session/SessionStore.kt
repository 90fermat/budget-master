package com.budgetmaster.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The signed-in user, as much as the app needs to show and scope by.
 *
 * @property displayName may be null — email/password sign-up never sets one, so callers must
 * fall back rather than assume a name exists.
 */
data class SessionUser(
    val id: String,
    val displayName: String?,
    val email: String?,
)

/**
 * Holds the signed-in user for the whole app.
 *
 * Lives in `:core` so feature repositories can scope their data by the current user, and
 * feature UI can greet them, without depending on `:feature:auth`. The composition root
 * observes the auth state and pushes the user here (and `null` on sign-out).
 */
class SessionStore {

    private val _currentUser = MutableStateFlow<SessionUser?>(null)

    /** The signed-in user, or `null` when nobody is authenticated. */
    val currentUser: StateFlow<SessionUser?> = _currentUser.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)

    /**
     * The signed-in user's id, or `null` when nobody is authenticated.
     *
     * Kept as its own flow rather than derived: repositories only care about identity, and
     * mapping [currentUser] would re-emit whenever an unrelated profile field changed.
     */
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    /** Sets (or clears, with `null`) the current user. */
    fun setCurrentUser(user: SessionUser?) {
        _currentUser.value = user
        _currentUserId.value = user?.id
    }
}
