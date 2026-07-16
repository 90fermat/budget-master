package com.budgetmaster.transactions.domain.repository

import com.budgetmaster.transactions.domain.model.RecurringDraft
import com.budgetmaster.transactions.domain.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow

/** Scheduled, repeating transactions for the signed-in user. */
interface RecurringRepository {

    /** Observes the current user's schedules across their wallets. */
    fun observeRecurring(): Flow<List<RecurringTransaction>>

    /** Creates or updates a schedule; returns its id. */
    suspend fun upsertRecurring(draft: RecurringDraft): String

    /** Pauses or resumes a schedule without losing it. */
    suspend fun setActive(id: String, active: Boolean)

    /** Deletes a schedule. Transactions it already produced are left alone. */
    suspend fun deleteRecurring(id: String)

    /**
     * Creates the transactions for every occurrence now due, advancing each schedule past
     * `now`. Idempotent per occurrence.
     *
     * @return how many transactions were created.
     */
    suspend fun materializeDue(now: Long): Int
}
