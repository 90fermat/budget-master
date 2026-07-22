@file:OptIn(ExperimentalUuidApi::class)

package com.budgetmaster.core.sync

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import com.budgetmaster.core.db.BudgetMasterDatabase
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.db.DefaultData
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * What signing in should do with the data written while signed out.
 *
 * The distinction that matters is between data the user *made* and data the app *seeded on their
 * behalf*. Discarding a starter wallet nobody typed into costs nothing; discarding a month of
 * transactions because the app decided a cloud account took precedence is unforgivable, and no
 * apology afterwards recovers it.
 */
sealed interface AdoptionPlan {
    /** Only the seeded starter wallet exists here. Take the cloud's data and say nothing. */
    data object AdoptCloud : AdoptionPlan

    /** Real local data, nothing in the cloud yet. Move it across; there is nothing to weigh it against. */
    data object AdoptLocal : AdoptionPlan

    /**
     * Both sides hold data the user made. This is the only case that must ask.
     *
     * Not because the code cannot pick — it could always merge — but because either side losing
     * data is a decision about the user's own records, and the app does not have the standing to
     * make it quietly.
     */
    data object AskUser : AdoptionPlan

    /** Nothing local worth moving and nothing in the cloud. Ordinary first sign-in. */
    data object Nothing : AdoptionPlan
}

/**
 * Whether sign-in should seed a starter wallet, given what adoption decided.
 *
 * The starter wallet exists for someone who has nothing, anywhere. Seeding it when the cloud is
 * about to supply real wallets leaves an empty "Cash" sitting beside them that the user never
 * created — and, because it is a local row like any other, the next push sends it to every other
 * device they own.
 *
 * A null [plan] means the remote could not be reached, so whether the cloud holds anything is
 * unknown. That also seeds nothing: an empty screen until the network returns is recoverable and
 * self-corrects on the next launch, whereas a spurious wallet has to be found and deleted on every
 * device it reached.
 */
fun shouldSeedStarterWallet(plan: AdoptionPlan?): Boolean = when (plan) {
    null, AdoptionPlan.AdoptCloud -> false
    // Local data is staying, so any wallet it had is still here and the seeder is a no-op anyway.
    AdoptionPlan.AdoptLocal, AdoptionPlan.AskUser -> true
    AdoptionPlan.Nothing -> true
}

/** What the user chose, when they were asked. */
enum class AdoptionChoice {
    /** Keep both sets. Local rows are re-keyed so they cannot collide with the cloud's. */
    Merge,

    /** Discard what is on this device and take the cloud's. */
    CloudOnly,

    /** Keep this device's data and retire what is in the cloud. */
    ThisDeviceOnly,
}

/**
 * Moves data written while signed out into the signed-in account.
 *
 * Local rows are owned by [DefaultData.DEFAULT_USER_ID] until this runs. Re-parenting is enough on
 * its own for accounts, budgets, goals and user-made categories; transactions need no attention at
 * all, because they hang off `accountId` rather than a user, so moving the accounts carries the
 * whole ledger with them.
 *
 * Default categories are never moved. `isDefault = 1` rows are seeded constants belonging to the
 * system user and shared by everybody, so re-parenting them would hand one account ownership of
 * every user's category list.
 *
 * The signed-in user's own row must already exist before any of this runs — `AppDataSeeder` writes
 * it at sign-in. Re-parenting points rows at that owner, and pointing them at an owner who is not
 * there is a foreign-key violation where constraints are enforced and a dangling reference where
 * they are not. The second is worse: it fails silently and shows up later as data belonging to
 * nobody.
 */
class LocalDataAdoption(
    private val databaseProvider: DatabaseProvider,
    private val newId: () -> String = { Uuid.random().toString() },
) {

    /** Decides what signing in as [uid] should do, given what is here and what the remote holds. */
    suspend fun plan(uid: String, remote: RemoteSyncDataSource?): AdoptionPlan {
        val hasLocal = hasUserData(DefaultData.DEFAULT_USER_ID)
        val hasRemote = remote != null && remote.hasAnyRecords()

        return when {
            hasLocal && hasRemote -> AdoptionPlan.AskUser
            hasLocal -> AdoptionPlan.AdoptLocal
            hasRemote -> AdoptionPlan.AdoptCloud
            else -> AdoptionPlan.Nothing
        }
    }

    /**
     * Whether this device holds anything the user actually made.
     *
     * The seeded starter wallet does not count on its own — first launch creates one, so treating
     * its presence as "real data" would make every first sign-in an interrogation. A transaction,
     * a budget, a goal, a second wallet or a category of their own all do count.
     */
    private suspend fun hasUserData(userId: String): Boolean {
        val q = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        if (q.countTransactionsByUser(userId).awaitAsOne() > 0) return true
        if (q.countBudgetsByUser(userId).awaitAsOne() > 0) return true
        if (q.countGoalsByUser(userId).awaitAsOne() > 0) return true
        if (q.countOwnCategoriesByUser(userId).awaitAsOne() > 0) return true
        return q.countAccountsByUser(userId).awaitAsOne() > 1
    }

    /** Carries out [plan], or the user's [choice] when the plan was to ask. */
    suspend fun apply(uid: String, plan: AdoptionPlan, choice: AdoptionChoice? = null) {
        when (plan) {
            AdoptionPlan.AdoptLocal -> reparent(uid)
            AdoptionPlan.AdoptCloud -> discardLocal(uid)
            // Nothing either side: the seeded starter wallet simply becomes theirs. The next
            // device to sign in will find the cloud non-empty and take it rather than adding a
            // second identical empty wallet.
            AdoptionPlan.Nothing -> reparent(uid)
            AdoptionPlan.AskUser -> when (choice) {
                AdoptionChoice.Merge -> mergeLocalIntoCloud(uid)
                AdoptionChoice.CloudOnly -> discardLocal(uid)
                AdoptionChoice.ThisDeviceOnly -> reparent(uid)
                null -> error("A plan of AskUser needs the user's choice before it can be applied")
            }
        }
    }

    /** Moves the anonymous rows to [uid], ids untouched. */
    private suspend fun reparent(uid: String) {
        val q = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        val from = DefaultData.DEFAULT_USER_ID
        q.reparentAccounts(uid, from)
        q.reparentOwnCategories(uid, from)
        q.reparentBudgets(uid, from)
        q.reparentGoals(uid, from)
    }

    /**
     * Moves the anonymous rows to [uid] under fresh ids.
     *
     * Re-keying is what makes merging safe. The seeded wallet's id is derived from the user id —
     * `default_user_cash` on every device that has ever been signed out — so two such devices
     * joining one account would write two different wallets to the same row and each would
     * overwrite the other, losing whichever synced first. Fresh ids make the two rows two rows.
     */
    private suspend fun mergeLocalIntoCloud(uid: String) {
        val q = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        val before = tombstoneKeys()
        val accounts = q.selectAccountsByUserId(DefaultData.DEFAULT_USER_ID).awaitAsList()

        accounts.forEach { account ->
            // Copy, repoint, remove — rather than updating the id in place. A row id cannot simply
            // be changed while other rows reference it: the children would dangle, and under
            // enforced foreign keys either order of two updates fails. Creating the replacement
            // first means the children are never without a parent.
            val fresh = newId()
            q.insertAccount(
                fresh, account.userId, account.name, account.type, account.balance,
                account.currency, account.createdAt, account.isArchived, account.includeInTotals,
            )
            q.rekeyAccountTransactions(fresh, account.id)
            q.rekeyRecurringForAccount(fresh, account.id)
            q.deleteAccount(account.id)
        }

        reparent(uid)
        // Removing the originals left tombstones naming ids the cloud may legitimately be using —
        // `default_user_cash` belongs to every device that was ever signed out. Publishing them
        // would delete another device's wallet during an operation whose entire purpose was to
        // lose nothing.
        clearTombstonesAddedSince(before)
    }

    private suspend fun tombstoneKeys(): Set<Pair<String, String>> =
        databaseProvider.getDatabase().budgetMasterDatabaseQueries
            .selectAllTombstones().awaitAsList().map { it.tableName to it.rowId }.toSet()

    private suspend fun clearTombstonesAddedSince(before: Set<Pair<String, String>>) {
        val q = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        q.selectAllTombstones().awaitAsList()
            .filterNot { (it.tableName to it.rowId) in before }
            .forEach { q.deleteTombstone(it.tableName, it.rowId) }
    }

    /**
     * Drops what is here, seeded or not, in favour of the cloud's.
     *
     * The deletes must not leave tombstones behind, and that is the whole difficulty. These rows
     * were written while signed out, so the cloud has never seen them and there is nothing there
     * to retract — but their ids are not unique to this device. The seeded wallet is
     * `default_user_cash` on every phone that has ever been signed out, so a tombstone naming it
     * would travel up and delete a *different* device's wallet that happens to share the id. The
     * user would have chosen "keep the cloud's data" and watched some of it disappear.
     *
     * So the tombstones this operation creates are removed again, identified by comparing against
     * a snapshot rather than by guessing which ids were touched: cascades reach rows this code
     * never names, and a list maintained by hand would fall out of step with the schema.
     */
    private suspend fun discardLocal(uid: String) {
        val q = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        val from = DefaultData.DEFAULT_USER_ID
        val before = tombstoneKeys()

        q.deleteBudgetsByUser(from)
        q.deleteGoalsByUser(from)
        q.deleteOwnCategoriesByUser(from)
        // Accounts cascade to their transactions, so the ledger goes with them.
        q.deleteAccountsByUser(from)

        clearTombstonesAddedSince(before)
    }
}
