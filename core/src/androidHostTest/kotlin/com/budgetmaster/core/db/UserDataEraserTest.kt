@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.core.db

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Deleting an account has to actually erase the ledger — Play requires it, and a "delete" that
 * leaves data behind is a data-safety violation. This proves the wipe reaches every user-scoped
 * table (including transactions that resolve through accounts) and leaves another user's data
 * untouched.
 */
class UserDataEraserTest {

    private fun db(): BudgetMasterDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BudgetMasterDatabase.Schema.synchronous().create(driver)
        return BudgetMasterDatabase(driver)
    }

    @Test
    fun `erases every trace of the target user and nothing of another`(): Unit = runBlocking {
        val database = db()
        val q = database.budgetMasterDatabaseQueries
        val farFuture = Long.MAX_VALUE

        // Two users, each with an account + a transaction on it, plus a category and a budget.
        q.insertUser("u1", "One", "u1@x.com", "USD", 0L)
        q.insertUser("u2", "Two", "u2@x.com", "USD", 0L)
        q.insertAccount("acc1", "u1", "Wallet 1", "CHECKING", 0.0, "USD", 0L, 0)
        q.insertAccount("acc2", "u2", "Wallet 2", "CHECKING", 0.0, "USD", 0L, 0)
        q.insertTransaction("t1", "acc1", "cat_food", -10.0, "u1 spend", 1L, null, null, 0, null)
        q.insertTransaction("t2", "acc2", "cat_food", -20.0, "u2 spend", 1L, null, null, 0, null)
        q.insertCategory("c1", "u1", "Food", "🍔", "#000000", 0)
        q.insertCategory("c2", "u2", "Food", "🍔", "#000000", 0)
        q.insertBudget("b1", "u1", "cat_food", 100.0, 0.0, 0L, farFuture)
        q.insertBudget("b2", "u2", "cat_food", 100.0, 0.0, 0L, farFuture)

        UserDataEraser(DatabaseProvider(database), Dispatchers.Unconfined).eraseAllData("u1")

        // u1 is gone everywhere...
        assertEquals(0, q.selectTransactionsByUser("u1").awaitAsList().size)
        assertEquals(0, q.selectAccountsByUserId("u1").awaitAsList().size)
        assertEquals(0, q.selectBudgetsByUserId("u1", 1L, 1L).awaitAsList().size)
        assertNull(q.selectUserById("u1").awaitAsOneOrNull())

        // ...and u2 is entirely intact.
        assertEquals(1, q.selectTransactionsByUser("u2").awaitAsList().size)
        assertEquals(1, q.selectAccountsByUserId("u2").awaitAsList().size)
        assertNotNull(q.selectUserById("u2").awaitAsOneOrNull())
    }

    @Test
    fun `erases the imported-message ledger`(): Unit = runBlocking {
        // This table was missed by the original wipe, so deleting an account left behind a record
        // of every mobile-money message the app had read: who sent it, when it arrived, and for
        // anything awaiting review the parsed amount and description. The query to clear it
        // already existed and simply was not called.
        val database = db()
        val q = database.budgetMasterDatabaseQueries
        q.insertUser("u1", "One", "u1@x.com", "USD", 0L)
        q.insertUser("u2", "Two", "u2@x.com", "USD", 0L)

        suspend fun seedMessage(hash: String, userId: String) = q.insertImportedMessage(
            hash = hash,
            userId = userId,
            provider = "orange_money",
            sender = "OrangeMoney",
            receivedAt = 1L,
            status = "PENDING_REVIEW",
            transactionId = null,
            externalId = "OM250717.1200.A1",
            // Kept only for a pending review, and the most sensitive part of the row.
            pendingAccountId = "acc1",
            pendingAmount = -20_244.0,
            pendingFee = 44.48,
            pendingDescription = "FOYANG CYRILLE",
            pendingOccurredAt = 1L,
        )
        seedMessage("h1", "u1")
        seedMessage("h2", "u2")

        UserDataEraser(DatabaseProvider(database), Dispatchers.Unconfined).eraseAllData("u1")

        assertNull(q.selectImportedMessageByHash("h1").awaitAsOneOrNull())
        assertNotNull(q.selectImportedMessageByHash("h2").awaitAsOneOrNull())
    }
}
