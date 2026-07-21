package com.budgetmaster.core.db

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration

/**
 * iOS implementation of [DatabaseDriverFactory].
 */
actual class DatabaseDriverFactory actual constructor() {
    actual suspend fun createDriver(): SqlDriver {
        // Foreign keys are off by default here too, so the schema's cascades would never fire.
        return NativeSqliteDriver(
            schema = BudgetMasterDatabase.Schema.synchronous(),
            name = "budget_master.db",
            onConfiguration = { configuration ->
                configuration.copy(
                    extendedConfig = configuration.extendedConfig.copy(foreignKeyConstraints = true),
                )
            },
        )
    }
}
