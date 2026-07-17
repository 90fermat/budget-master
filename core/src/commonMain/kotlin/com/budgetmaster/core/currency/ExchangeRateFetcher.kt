package com.budgetmaster.core.currency

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The subset of the open.er-api.com payload this app uses. */
@Serializable
internal data class ExchangeRateResponse(
    val result: String? = null,
    @SerialName("base_code") val baseCode: String? = null,
    val rates: Map<String, Double> = emptyMap(),
)

/**
 * Fetches currency rates from ExchangeRate-API's open endpoint.
 *
 * Chosen because it needs **no API key** — one embedded secret was enough — and because it covers
 * every code in [SUPPORTED_CURRENCY_CODES]. The ECB-backed free APIs (Frankfurter and friends)
 * are the obvious alternative but publish neither XAF nor NGN, which would leave two of the app's
 * six currencies permanently unconvertible.
 *
 * The upstream data refreshes once a day and the endpoint is rate-limited, which is why callers
 * go through [RefreshExchangeRatesUseCase] rather than fetching on demand.
 *
 * Attribution: the provider's terms require a visible "Rates By Exchange Rate API" credit, which
 * the accounts screen carries next to the net-worth total.
 */
class ExchangeRateFetcher(
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    },
    private val baseUrl: String = DEFAULT_BASE_URL,
) {
    /**
     * The latest rates for [base], or `null` if they can't be fetched.
     *
     * Returns null rather than throwing: rates are a nicety, and a network failure must never
     * take down a screen that works perfectly well from the cache.
     *
     * @return target code → rate to multiply a [base] amount by, restricted to
     *   [SUPPORTED_CURRENCY_CODES].
     */
    suspend fun fetchLatest(base: String): Map<String, Double>? = try {
        val response: HttpResponse = httpClient.get("$baseUrl/$base")
        if (!response.status.isSuccess()) {
            null
        } else {
            val body = response.body<ExchangeRateResponse>()
            if (body.result != "success") {
                null
            } else {
                // Storing all ~160 returned pairs would bloat the table for currencies the app
                // cannot even select.
                body.rates
                    .filterKeys { it in SUPPORTED_CURRENCY_CODES && !it.equals(base, ignoreCase = true) }
                    .takeIf { it.isNotEmpty() }
            }
        }
    } catch (e: Exception) {
        null
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://open.er-api.com/v6/latest"
    }
}
