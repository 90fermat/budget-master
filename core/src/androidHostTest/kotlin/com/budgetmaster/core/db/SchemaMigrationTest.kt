package com.budgetmaster.core.db

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
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

    /** A v2 database: v1 plus the columns 1.sqm added. The starting point for v2 → v3. */
    private fun v2Database(): SqlDriver = v1Database().apply {
        BudgetMasterDatabase.Schema.synchronous().migrate(this, 1, 2)
    }

    @Test
    fun migratingV2AddsTheImportColumnsAndTable() {
        val driver = v2Database()
        // Sanity: the starting point genuinely lacks them, or the test proves nothing.
        assertTrue("externalId" !in driver.columnsOf("TransactionEntity"))
        assertTrue("source" !in driver.columnsOf("TransactionEntity"))

        BudgetMasterDatabase.Schema.synchronous().migrate(driver, 2, 3)

        assertTrue("externalId" in driver.columnsOf("TransactionEntity"))
        assertTrue("source" in driver.columnsOf("TransactionEntity"))
        assertTrue("hash" in driver.columnsOf("ImportedMessageEntity"))
    }

    @Test
    fun migratingV2KeepsExistingEntriesAsManualWithNoExternalId() {
        val driver = v2Database()
        driver.exec(
            """
            INSERT INTO AccountEntity (id, userId, name, type, balance, currency, createdAt, isArchived)
            VALUES ('a1', 'u1', 'Cash', 'CASH', 25.0, 'XAF', 1000, 0)
            """.trimIndent(),
        )
        driver.exec(
            """
            INSERT INTO TransactionEntity (id, accountId, categoryId, amount, description, timestamp, notes, tags, isRecurring, transferGroupId)
            VALUES ('t1', 'a1', 'cat_food', -5.0, 'Coffee', 2000, NULL, NULL, 0, NULL)
            """.trimIndent(),
        )

        BudgetMasterDatabase.Schema.synchronous().migrate(driver, 2, 3)

        // Everything that already existed was typed by hand, so it must read as MANUAL — an
        // entry mislabelled as imported could be wiped by an "undo import".
        val source = driver.executeQuery(
            null,
            "SELECT source FROM TransactionEntity WHERE id = 't1'",
            { c -> c.next(); app.cash.sqldelight.db.QueryResult.Value(c.getString(0)) },
            0,
        ).value
        assertEquals("MANUAL", source)

        // And no provider id, so it can never collide with a future import's dedup key.
        val externalId = driver.executeQuery(
            null,
            "SELECT externalId FROM TransactionEntity WHERE id = 't1'",
            { c -> c.next(); app.cash.sqldelight.db.QueryResult.Value(c.getString(0)) },
            0,
        ).value
        assertNull(externalId)
    }

    /**
     * The path a long-installed device actually takes. Migrations are only ever tested one step
     * at a time otherwise, and a chain can break where the steps individually pass.
     */
    @Test
    fun migratingStraightFromV1ToV3Works() {
        val driver = v1Database()

        BudgetMasterDatabase.Schema.synchronous().migrate(driver, 1, 3)

        assertTrue("isArchived" in driver.columnsOf("AccountEntity"))
        assertTrue("transferGroupId" in driver.columnsOf("TransactionEntity"))
        assertTrue("externalId" in driver.columnsOf("TransactionEntity"))
        assertTrue("hash" in driver.columnsOf("ImportedMessageEntity"))
    }

    /** The partial unique index must allow many NULLs (every manual entry) but only one of each
     *  provider id — that asymmetry is the whole dedup guarantee. */
    @Test
    fun externalIdIsUniqueButManyRowsMayHaveNone() {
        val driver = v1Database()
        BudgetMasterDatabase.Schema.synchronous().migrate(driver, 1, 3)
        driver.exec(
            """
            INSERT INTO AccountEntity (id, userId, name, type, balance, currency, createdAt, isArchived)
            VALUES ('a1', 'u1', 'MoMo', 'CASH', 0.0, 'XAF', 1000, 0)
            """.trimIndent(),
        )

        fun insert(id: String, externalId: String?) = driver.exec(
            "INSERT INTO TransactionEntity (id, accountId, categoryId, amount, description, timestamp, " +
                "notes, tags, isRecurring, transferGroupId, externalId, source) VALUES " +
                "('$id', 'a1', NULL, -1.0, 'x', 1, NULL, NULL, 0, NULL, " +
                "${externalId?.let { "'$it'" } ?: "NULL"}, 'SMS')",
        )

        // Many manual rows, all with no provider id — must be allowed.
        insert("m1", null)
        insert("m2", null)

        insert("i1", "PP260621.0702.D51614")
        val duplicate = runCatching { insert("i2", "PP260621.0702.D51614") }
        assertTrue(duplicate.isFailure, "the same provider transaction must not import twice")
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
    fun migratingV3AddsTheReviewQueueColumns() {
        val driver = v1Database().apply {
            BudgetMasterDatabase.Schema.synchronous().migrate(this, 1, 3)
        }
        // Sanity: the starting point genuinely lacks them, or the test proves nothing.
        assertTrue("pendingAmount" !in driver.columnsOf("ImportedMessageEntity"))

        // A v3 row that was already awaiting review: it must survive, with NULL fields, because
        // there is nothing to back-fill from - the message body was never stored.
        driver.exec(
            """
            INSERT INTO ImportedMessageEntity (hash, userId, provider, sender, receivedAt, status, transactionId, externalId)
            VALUES ('h1', 'u1', 'orange_money', 'OrangeMoney', 100, 'PENDING_REVIEW', 't1', 'x1')
            """.trimIndent(),
        )

        BudgetMasterDatabase.Schema.synchronous().migrate(driver, 3, 4)

        val columns = driver.columnsOf("ImportedMessageEntity")
        listOf(
            "pendingAccountId", "pendingAmount", "pendingFee",
            "pendingDescription", "pendingOccurredAt",
        ).forEach { assertTrue(it in columns, "missing \$it") }

        val amount = driver.executeQuery(
            null,
            "SELECT pendingAmount FROM ImportedMessageEntity WHERE hash = 'h1'",
            { c -> c.next(); app.cash.sqldelight.db.QueryResult.Value(c.getDouble(0)) },
            0,
        ).value
        assertNull(amount)
    }

    @Test
    fun migratingV4AddsIncludeInTotalsDefaultingToOn() {
        val driver = v1Database().apply {
            BudgetMasterDatabase.Schema.synchronous().migrate(this, 1, 4)
        }
        assertTrue("includeInTotals" !in driver.columnsOf("AccountEntity"))

        // An account that existed before the column did.
        driver.exec(
            """
            INSERT INTO AccountEntity (id, userId, name, type, balance, currency, createdAt, isArchived)
            VALUES ('a_old', 'u1', 'Everyday', 'CASH', 10.0, 'XAF', 0, 0)
            """.trimIndent(),
        )

        BudgetMasterDatabase.Schema.synchronous().migrate(driver, 4, 5)

        assertTrue("includeInTotals" in driver.columnsOf("AccountEntity"))
        // Defaults to counted, so nobody's totals silently change on upgrade.
        val included = driver.executeQuery(
            null,
            "SELECT includeInTotals FROM AccountEntity WHERE id = 'a_old'",
            { c -> c.next(); app.cash.sqldelight.db.QueryResult.Value(c.getLong(0)) },
            0,
        ).value
        assertEquals(1L, included)
    }

    @Test
    fun freshDatabaseIsCreatedAtTheCurrentVersion() {
        // Guards the mistake that caused this: bumping the .sq without a matching migration
        // leaves fresh installs on a schema that upgraded installs can never reach.
        // v5 added AccountEntity.includeInTotals (4.sqm).
        assertEquals(5L, BudgetMasterDatabase.Schema.version)
    }
}
