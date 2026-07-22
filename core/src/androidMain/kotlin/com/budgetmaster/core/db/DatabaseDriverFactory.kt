package com.budgetmaster.core.db

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * Android implementation of [DatabaseDriverFactory].
 */
actual class DatabaseDriverFactory actual constructor() {
    actual suspend fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = BudgetMasterDatabase.Schema.synchronous(),
            context = AppContextHolder.context,
            name = "budget_master.db",
            callback = object : AndroidSqliteDriver.Callback(BudgetMasterDatabase.Schema.synchronous()) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    // Android leaves foreign keys off, and neither the framework nor SQLDelight
                    // turns them on. Every ON DELETE CASCADE in the schema was therefore inert:
                    // deleting a wallet left its transactions behind, unreachable through any
                    // query that joins accounts but still on disk — and, once sync exists, still
                    // published to every other device.
                    db.setForeignKeyConstraintsEnabled(true)
                }
            },
        )
    }
}
