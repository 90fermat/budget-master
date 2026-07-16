package com.budgetmaster.core.session

import com.budgetmaster.core.prefs.KeyValueStore
import kotlinx.coroutines.flow.Flow

/**
 * Persists which financial account (wallet) the app is currently scoped to.
 *
 * A `null` value means the consolidated **"All accounts"** view. Backed by [KeyValueStore]
 * (DataStore on Android/iOS, `localStorage` on Web) so the selection survives restarts.
 * Feature repositories combine their queries with [activeAccountId] to filter transactions.
 */
class ActiveAccountStore(private val store: KeyValueStore) {

    /** The selected account id, or `null` for the "All accounts" consolidated view. */
    val activeAccountId: Flow<String?> = store.observeString(KEY)

    /** Selects [id] as the active account, or clears back to "All accounts" with `null`. */
    suspend fun setActiveAccount(id: String?) {
        if (id == null) store.remove(KEY) else store.putString(KEY, id)
    }

    private companion object {
        const val KEY = "app.active_account"
    }
}
