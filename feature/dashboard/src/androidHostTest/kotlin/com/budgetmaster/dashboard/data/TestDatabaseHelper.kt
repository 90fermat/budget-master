@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.dashboard.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.budgetmaster.core.db.BudgetMasterDatabase

object TestDatabaseHelper {
    fun createInMemoryDatabase(): BudgetMasterDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BudgetMasterDatabase.Schema.synchronous().create(driver)
        return BudgetMasterDatabase(driver)
    }
}
