@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.core.currency

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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Guards the fetcher that finally populates the rate cache.
 *
 * Nothing wrote to `ExchangeRateEntity` before this existed, so any wallet in a second currency
 * was permanently unconvertible and net worth permanently "approximate".
 */
class ExchangeRateFetcherTest {

    private fun fetcher(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String,
        onRequest: (String) -> Unit = {},
    ) = ExchangeRateFetcher(
        httpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    onRequest(request.url.toString())
                    respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        },
    )

    @Test
    fun `keeps only supported currencies and drops the base itself`() = runBlocking {
        // The real endpoint returns ~160 pairs; storing them all would bloat the table with
        // currencies the app cannot even select.
        val body = """
            {"result":"success","base_code":"USD",
             "rates":{"USD":1.0,"EUR":0.92,"XAF":604.5,"NGN":1580.0,"JPY":157.2,"ZAR":18.4}}
        """.trimIndent()

        val rates = fetcher(body = body).fetchLatest("USD")

        assertEquals(setOf("EUR", "XAF", "NGN"), rates?.keys)
        assertEquals(0.92, rates?.get("EUR"))
    }

    @Test
    fun `requests the given base currency`() = runBlocking {
        var url = ""
        fetcher(body = """{"result":"success","base_code":"EUR","rates":{"USD":1.09}}""") { url = it }
            .fetchLatest("EUR")

        assertTrue(url.endsWith("/EUR"), "expected the base in the path, got $url")
    }

    /** Rates are a nicety: a failure must fall back to the cache, never take a screen down. */
    @Test
    fun `returns null on an http error rather than throwing`() = runBlocking {
        val rates = fetcher(status = HttpStatusCode.TooManyRequests, body = "rate limited")
            .fetchLatest("USD")

        assertNull(rates)
    }

    @Test
    fun `returns null when the provider reports a logical error`() = runBlocking {
        // HTTP 200 with result != success — the shape that would otherwise slip through.
        val rates = fetcher(body = """{"result":"error","error-type":"unsupported-code"}""")
            .fetchLatest("XXX")

        assertNull(rates)
    }

    @Test
    fun `returns null when nothing supported comes back`() = runBlocking {
        val rates = fetcher(body = """{"result":"success","base_code":"USD","rates":{"JPY":157.2}}""")
            .fetchLatest("USD")

        assertNull(rates, "no supported pairs is nothing to store, not an empty map to cache")
    }
}
