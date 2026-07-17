@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.core.currency

import com.budgetmaster.core.db.DatabaseProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The endpoint is rate-limited and its data only moves once a day, so refreshes are throttled. */
class RefreshExchangeRatesUseCaseTest {

    private lateinit var provider: DatabaseProvider
    private lateinit var repository: ExchangeRateRepository

    @BeforeTest
    fun setUp() {
        provider = DatabaseProvider(TestDatabaseHelper.createInMemoryDatabase())
        repository = ExchangeRateRepository(provider)
    }

    private var calls = 0

    private fun fetcher() = ExchangeRateFetcher(
        httpClient = HttpClient(MockEngine) {
            engine {
                addHandler {
                    calls++
                    respond(
                        """{"result":"success","base_code":"USD","rates":{"EUR":0.92,"XAF":604.5}}""",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        },
    )

    @Test
    fun `fetches and stores when nothing is cached`() = runBlocking {
        val refreshed = RefreshExchangeRatesUseCase(repository, fetcher()).invoke("USD")

        assertTrue(refreshed)
        assertEquals(0.92, repository.rate("USD", "EUR"))
        // The reciprocal still works off the stored pair, so EUR wallets convert too.
        assertEquals(1.0 / 0.92, repository.rate("EUR", "USD"))
    }

    @Test
    fun `does not fetch again while the cache is fresh`() = runBlocking {
        val useCase = RefreshExchangeRatesUseCase(repository, fetcher())
        useCase("USD")
        val callsAfterFirst = calls

        val refreshed = useCase("USD")

        assertFalse(refreshed)
        assertEquals(callsAfterFirst, calls, "a fresh cache must not hit a rate-limited endpoint")
    }

    @Test
    fun `fetches again once the cache is older than the max age`() = runBlocking {
        repository.putRate("USD", "EUR", 0.5)
        // maxAge 0 stands in for "whatever is cached is now too old", without sleeping.
        val refreshed = RefreshExchangeRatesUseCase(repository, fetcher(), maxAgeMs = 0).invoke("USD")

        assertTrue(refreshed)
        assertEquals(0.92, repository.rate("USD", "EUR"), "the stale rate should have been replaced")
    }

    /** Offline must be indistinguishable from online, minus fresh numbers. */
    @Test
    fun `keeps the cached rate when the fetch fails`() = runBlocking {
        repository.putRate("USD", "EUR", 0.5)
        val failing = ExchangeRateFetcher(
            httpClient = HttpClient(MockEngine) {
                engine { addHandler { respond("nope", HttpStatusCode.ServiceUnavailable) } }
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            },
        )

        val refreshed = RefreshExchangeRatesUseCase(repository, failing, maxAgeMs = 0).invoke("USD")

        assertFalse(refreshed)
        assertEquals(0.5, repository.rate("USD", "EUR"), "a failed fetch must not wipe the cache")
    }
}
