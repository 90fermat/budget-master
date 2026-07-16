@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.transactions.domain.usecase

import com.budgetmaster.transactions.domain.model.RecurringDraft
import com.budgetmaster.transactions.domain.model.RecurringTransaction
import com.budgetmaster.transactions.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Observes the user's recurring schedules. */
class ObserveRecurringUseCase(private val repository: RecurringRepository) {
    operator fun invoke(): Flow<List<RecurringTransaction>> = repository.observeRecurring()
}

/** Validates and saves a schedule. */
class SaveRecurringUseCase(private val repository: RecurringRepository) {
    /** @throws IllegalArgumentException if the amount is not positive or the label is blank. */
    suspend operator fun invoke(draft: RecurringDraft): String {
        require(draft.amountAbs > 0.0) { "Amount must be greater than zero." }
        require(draft.description.isNotBlank()) { "Description must not be blank." }
        return repository.upsertRecurring(draft)
    }
}

/** Pauses or resumes a schedule. */
class SetRecurringActiveUseCase(private val repository: RecurringRepository) {
    suspend operator fun invoke(id: String, active: Boolean) = repository.setActive(id, active)
}

/** Deletes a schedule, leaving the transactions it already produced in place. */
class DeleteRecurringUseCase(private val repository: RecurringRepository) {
    suspend operator fun invoke(id: String) = repository.deleteRecurring(id)
}

/**
 * Creates any transactions that have come due.
 *
 * Run at startup: there is no background scheduler, so the app catches up whenever it opens.
 * Occurrence ids are deterministic, so running it more often than needed is harmless.
 *
 * @return how many transactions were created.
 */
class MaterializeDueRecurringUseCase(private val repository: RecurringRepository) {
    suspend operator fun invoke(now: Long = Clock.System.now().toEpochMilliseconds()): Int =
        repository.materializeDue(now)
}
