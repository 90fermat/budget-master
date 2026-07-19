package com.budgetmaster.core.db

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Erases every local trace of a user's data — used when an account is deleted.
 *
 * Play requires an in-app account-deletion path for any app that has accounts, and "delete my
 * account" has to mean it: not just sign-out, but the ledger gone. Runs in a single transaction
 * so a failure can't leave a half-wiped database, and deletes transactions/recurring before
 * accounts because those resolve through the account rows.
 *
 * The shared insight cache is cleared too — it's derived from the user's spending, so it must not
 * survive them. The generic exchange-rate cache is market data, not personal, and is left.
 *
 * The imported-message ledger is erased as well. It was missed originally, which meant deleting an
 * account left behind a record of every mobile-money message the app had read — sender, arrival
 * time, and for anything awaiting review the parsed amount and description. That is arguably the
 * most invasive table in the database, and the query to clear it already existed for this purpose.
 */
class UserDataEraser(
    private val databaseProvider: DatabaseProvider,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    suspend fun eraseAllData(userId: String) = withContext(dispatcher) {
        val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        queries.transaction {
            queries.deleteTransactionsByUser(userId)
            queries.deleteRecurringByUser(userId)
            queries.deleteAccountsByUser(userId)
            queries.deleteCategoriesByUser(userId)
            queries.deleteBudgetsByUser(userId)
            queries.deleteGoalsByUser(userId)
            queries.deleteNotificationsByUser(userId)
            queries.deleteImportedMessagesByUser(userId)
            queries.deleteAllInsights()
            queries.deleteUser(userId)
        }
    }
}
