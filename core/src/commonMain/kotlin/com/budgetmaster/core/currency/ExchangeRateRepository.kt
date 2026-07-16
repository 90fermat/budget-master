@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.core.currency

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import com.budgetmaster.core.db.DatabaseProvider
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Reads and caches currency conversion rates in `ExchangeRateEntity`.
 *
 * Rates are stored per (base → target) pair with the time they were recorded, so conversion
 * works offline from the last known values. A missing pair returns `null` rather than
 * guessing — callers surface the total as approximate instead of inventing a number.
 */
class ExchangeRateRepository(private val databaseProvider: DatabaseProvider) {

    /**
     * The rate to multiply a [base] amount by to get [target], or `null` if unknown.
     *
     * Identical currencies convert 1:1. An inverse pair is used when only the opposite
     * direction has been stored.
     */
    suspend fun rate(base: String, target: String): Double? {
        if (base.equals(target, ignoreCase = true)) return 1.0
        val queries = databaseProvider.getDatabase().budgetMasterDatabaseQueries
        queries.selectExchangeRate(base, target).awaitAsOneOrNull()?.let { return it.rate }
        // Fall back to the reciprocal of the reverse pair if that is what we happen to hold.
        queries.selectExchangeRate(target, base).awaitAsOneOrNull()?.let {
            if (it.rate != 0.0) return 1.0 / it.rate
        }
        return null
    }

    /** Converts [amount] from [base] to [target], or `null` when no rate is known. */
    suspend fun convert(amount: Double, base: String, target: String): Double? =
        rate(base, target)?.let { amount * it }

    /** Stores (or refreshes) a conversion rate. */
    suspend fun putRate(base: String, target: String, rate: Double) {
        databaseProvider.getDatabase().budgetMasterDatabaseQueries.insertExchangeRate(
            baseCurrency = base,
            targetCurrency = target,
            rate = rate,
            timestamp = Clock.System.now().toEpochMilliseconds(),
        )
    }
}
