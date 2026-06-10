package com.budgetmaster.core.db

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * iOS implementation of [DatabaseDriverFactory].
 */
actual class DatabaseDriverFactory actual constructor() {
    actual suspend fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = BudgetMasterDatabase.Schema.synchronous(),
            name = "budget_master.db"
        )
    }
}
