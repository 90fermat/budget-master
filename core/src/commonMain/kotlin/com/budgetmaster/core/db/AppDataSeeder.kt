@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.core.db

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Seeds per-user data so every feature's foreign keys resolve.
 *
 * - **Default categories** are shared across all users (`isDefault = 1`) and owned by the
 *   system [DefaultData.DEFAULT_USER_ID]; seeded once.
 * - For each signed-in user, a [UserEntity] row and a first **"Cash"** wallet are created if
 *   missing. Idempotent per user id and safe to call from multiple callers — a [Mutex]
 *   guards concurrent seeding.
 */
class AppDataSeeder(private val databaseProvider: DatabaseProvider) {

    private val mutex = Mutex()
    private val seededUsers = mutableSetOf<String>()
    private var categoriesSeeded = false

    /**
     * Ensures the system user + shared default categories exist, then creates [userId]'s
     * user row and first wallet if absent.
     */
    suspend fun seedForUser(
        userId: String,
        email: String = "local@budgetmaster.app",
        name: String = "You",
    ) {
        if (userId in seededUsers) return
        mutex.withLock {
            if (userId in seededUsers) return
            val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
            val now = Clock.System.now().toEpochMilliseconds()

            seedCategoriesLocked(now)

            if (queries.selectUserById(userId).awaitAsOneOrNull() == null) {
                queries.insertUser(
                    id = userId,
                    name = name,
                    email = email,
                    currency = DefaultData.DEFAULT_CURRENCY,
                    createdAt = now,
                )
            }
            val accountCount = queries.countAccountsByUserId(userId).awaitAsOneOrNull() ?: 0L
            if (accountCount == 0L) {
                queries.insertAccount(
                    id = DefaultData.firstAccountId(userId),
                    userId = userId,
                    name = "Cash",
                    type = "CASH",
                    balance = 0.0,
                    currency = DefaultData.DEFAULT_CURRENCY,
                    createdAt = now,
                    isArchived = 0,
                )
            }
            seededUsers.add(userId)
        }
    }

    /**
     * Backwards-compatible entry point: seeds the fallback [DefaultData.DEFAULT_USER_ID]
     * (used by tests and by local mode before a session is bound).
     */
    suspend fun seedIfNeeded() = seedForUser(DefaultData.DEFAULT_USER_ID)

    private suspend fun seedCategoriesLocked(now: Long) {
        if (categoriesSeeded) return
        val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        if (queries.selectUserById(DefaultData.DEFAULT_USER_ID).awaitAsOneOrNull() == null) {
            queries.insertUser(
                id = DefaultData.DEFAULT_USER_ID,
                name = "System",
                email = "system@budgetmaster.app",
                currency = DefaultData.DEFAULT_CURRENCY,
                createdAt = now,
            )
        }
        val existing = queries.selectCategoriesByUserId(DefaultData.DEFAULT_USER_ID).awaitAsList()
        if (existing.none { it.isDefault == 1L }) {
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
        categoriesSeeded = true
    }
}
