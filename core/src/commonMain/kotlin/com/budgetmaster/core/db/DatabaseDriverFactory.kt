package com.budgetmaster.core.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Platform-specific factory for creating [SqlDriver] instances.
 */
expect class DatabaseDriverFactory() {
    /**
     * Creates and returns a platform-specific [SqlDriver].
     */
    suspend fun createDriver(): SqlDriver
}
