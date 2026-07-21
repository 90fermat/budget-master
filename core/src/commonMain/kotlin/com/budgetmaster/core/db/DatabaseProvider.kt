package com.budgetmaster.core.db

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe provider for lazy, asynchronous initialization of [BudgetMasterDatabase].
 */
class DatabaseProvider(private val driverFactory: DatabaseDriverFactory) {
    private val mutex = Mutex()
    private var database: BudgetMasterDatabase? = null

    /**
     * Secondary constructor for testing to inject a pre-configured database instance.
     */
    constructor(testDatabase: BudgetMasterDatabase) : this(DatabaseDriverFactory()) {
        this.database = testDatabase
    }

    /**
     * Returns the initialized [BudgetMasterDatabase] instance.
     */
    suspend fun getDatabase(): BudgetMasterDatabase {
        return database ?: mutex.withLock {
            database ?: run {
                val driver = driverFactory.createDriver()
                // Installed here rather than in the schema, because SQLDelight cannot compile a
                // trigger body referencing NEW/OLD. Idempotent (`IF NOT EXISTS`) and cheap, so
                // running it on every open covers fresh databases and migrated ones alike without
                // needing to know which just happened.
                SyncTriggers.install(driver)
                BudgetMasterDatabase(driver).also { database = it }
            }
        }
    }
}
