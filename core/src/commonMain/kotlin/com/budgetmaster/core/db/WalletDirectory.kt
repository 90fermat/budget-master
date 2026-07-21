@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.budgetmaster.core.db

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.budgetmaster.core.session.SessionStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** The minimum a cross-feature consumer needs to identify a wallet. */
data class WalletRef(val id: String, val name: String)

/**
 * Read-only wallet lookup for consumers outside `:feature:accounts`.
 *
 * Exists because the architecture forbids feature→feature imports: Settings needs a wallet list
 * for the import-destination picker, and the SMS importer needs to turn an account id into a name
 * for a notification, but neither may depend on the accounts feature. `:core` owns the database,
 * so a thin projection here is the sanctioned path. Deliberately id+name only — anything richer
 * (balances, archival state) belongs to the feature.
 */
class WalletDirectory(
    private val databaseProvider: DatabaseProvider,
    private val sessionStore: SessionStore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    /** Active (non-archived) wallets for the current user, in creation order. */
    fun observeWallets(): Flow<List<WalletRef>> =
        sessionStore.currentUserId.flatMapLatest { uid ->
            val userId = uid ?: DefaultData.DEFAULT_USER_ID
            flow {
                val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
                emitAll(
                    queries.selectActiveAccountsByUserId(userId)
                        .asFlow().mapToList(dispatcher)
                        .map { rows -> rows.map { WalletRef(it.id, it.name) } },
                )
            }
        }
}
