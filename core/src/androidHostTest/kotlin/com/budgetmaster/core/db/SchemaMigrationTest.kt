package com.budgetmaster.core.db

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the v1 → v2 migration.
 *
 * This exists because the schema was edited in place three times without a version bump, so
 * "v1" briefly described several different databases and an existing install crashed with
 * "no such column". Recreating the original v1 here and migrating it is the only way to know
 * the upgrade path actually works — creating a fresh database proves nothing, because
 * `Schema.create()` always emits the newest columns.
 */
class SchemaMigrationTest {

    /** The AccountEntity/TransactionEntity shape exactly as v1 shipped it. */
    private val v1Account = """
        CREATE TABLE AccountEntity (
            id TEXT NOT NULL PRIMARY KEY,
            userId TEXT NOT NULL,
            name TEXT NOT NULL,
            type TEXT NOT NULL,
            balance REAL NOT NULL,
            currency TEXT NOT NULL,
            createdAt INTEGER NOT NULL
        )
    """.trimIndent()

    private val v1Transaction = """
        CREATE TABLE TransactionEntity (
            id TEXT NOT NULL PRIMARY KEY,
            accountId TEXT NOT NULL,
            categoryId TEXT,
            amount REAL NOT NULL,
            description TEXT NOT NULL,
            timestamp INTEGER NOT NULL,
            notes TEXT,
            tags TEXT,
            isRecurring INTEGER NOT NULL DEFAULT 0
        )
    """.trimIndent()

    private fun SqlDriver.exec(sql: String) = execute(null, sql, 0).value

    private fun SqlDriver.columnsOf(table: String): List<String> =
        executeQuery(
            identifier = null,
            sql = "PRAGMA table_info($table)",
            mapper = { cursor ->
                val names = mutableListOf<String>()
                while (cursor.next().value) names += cursor.getString(1).orEmpty()
                app.cash.sqldelight.db.QueryResult.Value(names.toList())
            },
            parameters = 0,
        ).value

    private fun v1Database(): SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).apply {
        exec(v1Account)
        exec(v1Transaction)
    }

    @Test
    fun migratingV1AddsTheNewColumns() {
        val driver = v1Database()
        // Sanity: the starting point genuinely lacks them, or the test proves nothing.
        assertTrue("isArchived" !in driver.columnsOf("AccountEntity"))
        assertTrue("transferGroupId" !in driver.columnsOf("TransactionEntity"))

        BudgetMasterDatabase.Schema.synchronous().migrate(driver, 1, 2)

        assertTrue("isArchived" in driver.columnsOf("AccountEntity"))
        assertTrue("transferGroupId" in driver.columnsOf("TransactionEntity"))
    }

    @Test
    fun migrationPreservesExistingRowsWithSafeDefaults() {
        val driver = v1Database()
        driver.exec(
            """
            INSERT INTO AccountEntity (id, userId, name, type, balance, currency, createdAt)
            VALUES ('a1', 'u1', 'Cash', 'CASH', 25.0, 'USD', 1000)
            """.trimIndent(),
        )
        driver.exec(
            """
            INSERT INTO TransactionEntity (id, accountId, categoryId, amount, description, timestamp, notes, tags, isRecurring)
            VALUES ('t1', 'a1', 'cat_food', -5.0, 'Coffee', 2000, NULL, NULL, 0)
            """.trimIndent(),
        )

        BudgetMasterDatabase.Schema.synchronous().migrate(driver, 1, 2)

        // The wallet survives and defaults to active rather than vanishing from the list.
        val archived = driver.executeQuery(
            null,
            "SELECT isArchived FROM AccountEntity WHERE id = 'a1'",
            { c -> c.next(); app.cash.sqldelight.db.QueryResult.Value(c.getLong(0)) },
            0,
        ).value
        assertEquals(0L, archived)

        // The entry survives as a normal expense, not a transfer leg — which is what the
        // report and budget filters assume when they exclude transfers.
        val group = driver.executeQuery(
            null,
            "SELECT transferGroupId FROM TransactionEntity WHERE id = 't1'",
            { c -> c.next(); app.cash.sqldelight.db.QueryResult.Value(c.getString(0)) },
            0,
        ).value
        assertNull(group)
    }

    @Test
    fun freshDatabaseIsCreatedAtTheCurrentVersion() {
        // Guards the mistake that caused this: bumping the .sq without a matching migration
        // leaves fresh installs on a schema that upgraded installs can never reach.
        assertEquals(2L, BudgetMasterDatabase.Schema.version)
    }
}
