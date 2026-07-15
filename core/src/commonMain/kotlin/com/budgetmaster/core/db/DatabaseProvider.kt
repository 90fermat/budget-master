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
                BudgetMasterDatabase(driver).also { database = it }
            }
        }
    }
}
