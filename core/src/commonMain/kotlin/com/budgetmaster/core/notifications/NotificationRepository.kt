@file:OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)

package com.budgetmaster.core.notifications

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.db.DefaultData
import com.budgetmaster.core.session.SessionStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** An in-app notification, scoped to the signed-in user. */
data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean,
)

/**
 * Stores in-app notifications in `NotificationEntity`, scoped to the current user.
 *
 * This is the persistence half of notifications. Delivering them to the OS notification
 * shade is platform work that lands with the Phase 3 recurring engine; everything here is
 * already usable for an in-app inbox and badge.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationRepository(
    private val databaseProvider: DatabaseProvider,
    private val sessionStore: SessionStore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    private fun currentUserId(): String = sessionStore.currentUserId.value ?: DefaultData.DEFAULT_USER_ID

    /** All notifications for the current user, newest first. */
    fun observeAll(): Flow<List<AppNotification>> =
        sessionStore.currentUserId.flatMapLatest { uid ->
            val userId = uid ?: DefaultData.DEFAULT_USER_ID
            flow {
                val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
                emitAll(
                    queries.selectAllNotificationsByUserId(userId)
                        .asFlow().mapToList(dispatcher)
                        .map { rows ->
                            rows.map {
                                AppNotification(it.id, it.title, it.message, it.timestamp, it.isRead == 1L)
                            }
                        },
                )
            }
        }

    /** Unread count, for a badge. */
    fun observeUnreadCount(): Flow<Int> = observeAll().map { list -> list.count { !it.isRead } }

    /**
     * Records a notification.
     *
     * @param id stable id — reusing one (e.g. a budget-threshold key) makes this idempotent,
     * so the same alert can't pile up on every recomposition or app launch.
     */
    suspend fun notify(title: String, message: String, id: String = Uuid.random().toString()): Unit =
        withContext(dispatcher) {
            databaseProvider.getDatabase().budgetMasterDatabaseQueries.insertNotification(
                id = id,
                userId = currentUserId(),
                title = title,
                message = message,
                timestamp = Clock.System.now().toEpochMilliseconds(),
                isRead = 0,
            )
        }

    suspend fun markRead(id: String): Unit = withContext(dispatcher) {
        databaseProvider.getDatabase().budgetMasterDatabaseQueries.markNotificationRead(id)
    }

    suspend fun delete(id: String): Unit = withContext(dispatcher) {
        databaseProvider.getDatabase().budgetMasterDatabaseQueries.deleteNotification(id)
    }
}
