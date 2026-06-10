package com.budgetmaster.core.db

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * Android implementation of [DatabaseDriverFactory].
 */
actual class DatabaseDriverFactory actual constructor() {
    actual suspend fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = BudgetMasterDatabase.Schema.synchronous(),
            context = AppContextHolder.context,
            name = "budget_master.db"
        )
    }
}
