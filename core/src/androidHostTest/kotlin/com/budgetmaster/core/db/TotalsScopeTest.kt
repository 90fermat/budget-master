package com.budgetmaster.core.db

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The consolidated "All accounts" view must leave out wallets the user keeps separate.
 *
 * Tested at the query, because that is where the rule lives — every consolidated surface
 * (dashboard, transactions list, reports, CSV export) goes through these two queries, so proving
 * them proves all of it, and no future caller can forget to apply the filter.
 */
class TotalsScopeTest {

    private fun database(): BudgetMasterDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BudgetMasterDatabase.Schema.synchronous().create(driver)
        return BudgetMasterDatabase(driver)
    }

    /** Everyday money and an Epargne wallet the user wants kept apart — the reported case. */
    private suspend fun seed(db: BudgetMasterDatabase) {
        val q = db.budgetMasterDatabaseQueries
        q.insertUser("u1", "Cyrille", "c@example.com", "XAF", 0L)
        q.insertAccount("everyday", "u1", "Everyday", "CASH", 0.0, "XAF", 0L, 0, 1)
        q.insertAccount("epargne", "u1", "Epargne", "SAVINGS", 0.0, "XAF", 0L, 0, 0)
        q.insertTransaction("t_everyday", "everyday", null, -1_000.0, "Lunch", 10L, null, null, 0, null)
        q.insertTransaction("t_epargne", "epargne", null, -50_000.0, "Savings move", 20L, null, null, 0, null)
    }

    @Test
    fun `the consolidated list omits an excluded wallet`(): Unit = runBlocking {
        val db = database()
        seed(db)

        val consolidated = db.budgetMasterDatabaseQueries.selectTransactionsByUser("u1").awaitAsList()

        assertEquals(1, consolidated.size)
        assertEquals("t_everyday", consolidated.single().id)
        assertTrue(consolidated.none { it.accountId == "epargne" }, "Epargne must stay out")
    }

    @Test
    fun `the paged consolidated list omits it too`(): Unit = runBlocking {
        val db = database()
        seed(db)

        // The list uses the paged variant; if only the unpaged one were filtered, the balance and
        // the visible rows would disagree — which is worse than not filtering at all.
        val paged = db.budgetMasterDatabaseQueries.selectTransactionsByUserPaged("u1", 50).awaitAsList()

        assertEquals(1, paged.size)
        assertEquals("t_everyday", paged.single().id)
    }

    @Test
    fun `the excluded wallet is still listed and still readable on its own`(): Unit = runBlocking {
        val db = database()
        seed(db)
        val q = db.budgetMasterDatabaseQueries

        // Excluded from totals is not hidden: the wallet still appears in the accounts list...
        val accounts = q.selectAccountsByUserId("u1").awaitAsList()
        assertEquals(2, accounts.size)
        assertTrue(accounts.any { it.id == "epargne" })

        // ...and switching to it explicitly still shows its transactions.
        val itsOwn = q.selectTransactionsByAccount("epargne").awaitAsList()
        assertEquals(1, itsOwn.size)
        assertEquals("t_epargne", itsOwn.single().id)
    }

    @Test
    fun `including it again brings it back into the consolidated view`(): Unit = runBlocking {
        val db = database()
        seed(db)
        val q = db.budgetMasterDatabaseQueries

        q.setAccountIncludedInTotals(includeInTotals = 1, id = "epargne")

        assertEquals(2, q.selectTransactionsByUser("u1").awaitAsList().size)
    }
}
