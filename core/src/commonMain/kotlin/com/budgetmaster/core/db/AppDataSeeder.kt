@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.core.db

import app.cash.sqldelight.async.coroutines.awaitAsList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Seeds the default user, account, and categories exactly once so every feature's
 * foreign keys resolve. Idempotent and safe to call from multiple repositories and
 * from app startup — a [Mutex] guards concurrent first-run seeding.
 */
class AppDataSeeder(private val databaseProvider: DatabaseProvider) {

    private val mutex = Mutex()
    private var seeded = false

    /** Inserts the default user/account/categories if the categories are not yet present. */
    suspend fun seedIfNeeded() {
        if (seeded) return
        mutex.withLock {
            if (seeded) return
            val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
            val existing = queries.selectCategoriesByUserId(DefaultData.DEFAULT_USER_ID).awaitAsList()
            if (existing.isEmpty()) {
                val now = Clock.System.now().toEpochMilliseconds()
                queries.insertUser(
                    id = DefaultData.DEFAULT_USER_ID,
                    name = "You",
                    email = "local@budgetmaster.app",
                    currency = DefaultData.DEFAULT_CURRENCY,
                    createdAt = now,
                )
                queries.insertAccount(
                    id = DefaultData.DEFAULT_ACCOUNT_ID,
                    userId = DefaultData.DEFAULT_USER_ID,
                    name = "Main Account",
                    type = "CHECKING",
                    balance = 0.0,
                    currency = DefaultData.DEFAULT_CURRENCY,
                    createdAt = now,
                )
                DefaultData.categories.forEach { cat ->
                    queries.insertCategory(
                        id = cat.id,
                        userId = DefaultData.DEFAULT_USER_ID,
                        name = cat.name,
                        icon = cat.icon,
                        color = cat.colorHex,
                        isDefault = 1,
                    )
                }
            }
            seeded = true
        }
    }
}
