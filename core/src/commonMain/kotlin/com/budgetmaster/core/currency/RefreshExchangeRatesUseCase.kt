@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.core.currency

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Keeps the local rate cache fresh enough for multi-currency totals to mean something.
 *
 * Until this existed, `ExchangeRateEntity` was only ever read: nothing wrote to it, so any wallet
 * in a second currency was permanently unconvertible and net worth was permanently "approximate".
 *
 * Fetches at most once per [maxAgeMs] because the upstream data updates daily and the free
 * endpoint is rate-limited. Failure is silent by design — conversion falls back to the last known
 * rates, and an offline launch must not be slower or noisier than an online one.
 */
class RefreshExchangeRatesUseCase(
    private val repository: ExchangeRateRepository,
    private val fetcher: ExchangeRateFetcher,
    private val maxAgeMs: Long = DEFAULT_MAX_AGE_MS,
) {
    /**
     * Refreshes [base]'s rates if they're missing or older than [maxAgeMs].
     *
     * @return true if new rates were stored.
     */
    suspend operator fun invoke(base: String): Boolean {
        val lastUpdated = repository.lastUpdated(base)
        val now = Clock.System.now().toEpochMilliseconds()
        if (lastUpdated != null && now - lastUpdated < maxAgeMs) return false

        val rates = fetcher.fetchLatest(base) ?: return false
        rates.forEach { (target, rate) -> repository.putRate(base, target, rate) }
        return true
    }

    private companion object {
        const val DEFAULT_MAX_AGE_MS = 24 * 60 * 60 * 1000L
    }
}
