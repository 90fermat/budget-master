package com.budgetmaster.transactions.domain.usecase

import com.budgetmaster.transactions.domain.repository.MoneyImportRepository
import com.budgetmaster.transactions.domain.repository.PendingImport
import kotlinx.coroutines.flow.Flow

/**
 * The review queue: captured messages that look like something the user already entered by hand.
 *
 * Empty for almost everyone, almost always — the importer only defers when it genuinely cannot
 * tell, and the UI is expected to disappear entirely when the list is empty.
 */
class ObservePendingImportsUseCase(
    private val repository: MoneyImportRepository,
) {
    operator fun invoke(): Flow<List<PendingImport>> = repository.observePendingReview()
}

/**
 * Answers one review.
 *
 * @param keep true when it is a genuinely separate transaction, which writes the entries the
 *   importer held back; false when it is the same event, which records the duplicate and writes
 *   nothing. Either answer closes the question for good, so it is never asked twice.
 */
class ResolvePendingImportUseCase(
    private val repository: MoneyImportRepository,
) {
    suspend operator fun invoke(hash: String, keep: Boolean): List<String> =
        repository.resolvePending(hash, keep)
}
