package com.budgetmaster.core.currency

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.budgetmaster.core.db.BudgetMasterDatabase

/**
 * A fresh in-memory database per test.
 *
 * `Schema.create()` emits the current schema, which is right for tests about behaviour. Tests
 * about *migrations* must not use this — see `SchemaMigrationTest`, which builds the old schema
 * by hand precisely because `create()` would hide the bug it guards.
 */
internal object TestDatabaseHelper {
    fun createInMemoryDatabase(): BudgetMasterDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BudgetMasterDatabase.Schema.synchronous().create(driver)
        return BudgetMasterDatabase(driver)
    }
}
