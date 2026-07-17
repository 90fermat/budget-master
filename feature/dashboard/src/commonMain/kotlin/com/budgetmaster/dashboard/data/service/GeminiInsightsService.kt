@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.dashboard.data.service

import app.cash.sqldelight.async.coroutines.awaitAsList
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.model.Transaction
import com.budgetmaster.dashboard.config.BuildConfig
import com.budgetmaster.dashboard.domain.model.Insight
import com.budgetmaster.dashboard.domain.model.InsightType
import com.budgetmaster.dashboard.data.remote.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.withTimeout

import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Service calling the Gemini API to analyze transactions and generate spending insights.
 * Implements local caching with SQLDelight and error resilience (timeout, rate limiting).
 *
 * **Only aggregates leave the device.** The prompt carries per-category totals and the period's
 * income/expense sums — never descriptions (free text, and users put names in them), timestamps,
 * or ids. Sending the raw ledger to a third party is not something a finance app should do, and
 * the aggregates are what the model actually reasons about anyway.
 *
 * When no API key is configured the service is [isConfigured] `false` and returns nothing: it
 * used to answer with hardcoded "mock" insights that stated invented figures ("coffee spending
 * up 15%") as though they were real analysis of the user's money.
 */
class GeminiInsightsService(
    private val databaseProvider: DatabaseProvider,
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    },
    private val apiKeyProvider: () -> String = { BuildConfig.GEMINI_API_KEY }
) {
    private val jsonConfiguration = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    /**
     * Whether a usable API key is present. False in any build that did not supply one — which is
     * every release build, since shipping the key would put it in the bundle for anyone to
     * extract. Callers should hide the AI surface entirely rather than show an empty section.
     */
    val isConfigured: Boolean
        get() = apiKeyProvider().let { key ->
            key.isNotBlank() && key !in PLACEHOLDER_KEYS
        }

    /**
     * Retrieves insights either from local cache (if age < 24h) or generates them via Gemini.
     * Fallbacks to cache in case of rate-limiting or network issues.
     */
    suspend fun getInsights(
        transactions: List<Transaction>,
        forceRefresh: Boolean,
        languageTag: String = "en"
    ): List<Insight> {
        val db = databaseProvider.getDatabase()
        val queries = db.budgetMasterDatabaseQueries

        // 1. Check local cache first
        val cachedEntities = queries.selectAllInsights().awaitAsList()
        val now = Clock.System.now().toEpochMilliseconds()
        val cacheAgeLimit = 24 * 60 * 60 * 1000L // 24 hours in ms

        if (cachedEntities.isNotEmpty() && !forceRefresh) {
            val oldestTimestamp = cachedEntities.minOf { it.timestamp }
            if (now - oldestTimestamp < cacheAgeLimit) {
                return cachedEntities.map { entity ->
                    Insight(
                        id = entity.id,
                        type = mapStringToType(entity.type),
                        message = entity.message,
                        actionRoute = entity.actionRoute,
                        generatedAt = Instant.fromEpochMilliseconds(entity.timestamp)
                    )
                }
            }
        }

        // 2. Fetch from Gemini API
        try {
            val apiKey = apiKeyProvider()
            if (!isConfigured) {
                // No key: answer with the cache if we have one, otherwise nothing at all. Never
                // invent insights — a fabricated "your coffee spending rose 15%" is worse than
                // silence in an app whose whole job is telling the user the truth about money.
                return cachedEntities.map { entity ->
                    Insight(
                        id = entity.id,
                        type = mapStringToType(entity.type),
                        message = entity.message,
                        actionRoute = entity.actionRoute,
                        generatedAt = Instant.fromEpochMilliseconds(entity.timestamp)
                    )
                }
            }

            val prompt = buildPrompt(transactions, languageTag)

            val responseText = withTimeout(10000L) { // 10 seconds timeout
                val httpResponse: HttpResponse = httpClient.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent") {
                    parameter("key", apiKey)
                    contentType(ContentType.Application.Json)
                    setBody(
                        GeminiRequest(
                            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                            generationConfig = GenerationConfig(responseMimeType = "application/json")
                        )
                    )
                }
                
                if (httpResponse.status == HttpStatusCode.TooManyRequests) {
                    println("WARNING: Gemini API Rate Limit Exceeded (HTTP 429).")
                    throw RateLimitException()
                }

                if (!httpResponse.status.isSuccess()) {
                    throw ApiException("HTTP error: ${httpResponse.status}")
                }

                val geminiResponse = httpResponse.body<GeminiResponse>()
                val jsonString = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw ParseException("Empty response from Gemini")
                jsonString
            }

            // Parse response json string to DTOs
            val dtos = jsonConfiguration.decodeFromString<List<GeminiInsightDto>>(responseText)
            
            // Map DTOs to Domain models and update local cache
            val newInsights = dtos.mapIndexed { index, dto ->
                Insight(
                    id = "insight_${now}_$index",
                    type = mapStringToType(dto.type),
                    message = dto.message,
                    actionRoute = dto.actionRoute,
                    generatedAt = Clock.System.now()
                )
            }

            // Update database cache
            queries.deleteAllInsights()
            newInsights.forEach { insight ->
                queries.insertInsight(
                    id = insight.id,
                    type = insight.type.name,
                    message = insight.message,
                    actionRoute = insight.actionRoute,
                    timestamp = now
                )
            }

            return newInsights

        } catch (e: Exception) {
            println("Exception in GeminiInsightsService: ${e.message}")
            if (e is RateLimitException) {
                // Rate limit -> Return cached
                if (cachedEntities.isNotEmpty()) {
                    return cachedEntities.map { entity ->
                        Insight(
                            id = entity.id,
                            type = mapStringToType(entity.type),
                            message = entity.message,
                            actionRoute = entity.actionRoute,
                            generatedAt = Instant.fromEpochMilliseconds(entity.timestamp)
                        )
                    }
                }
            }

            // Network timeout / other exception -> Return cached if available
            if (cachedEntities.isNotEmpty()) {
                return cachedEntities.map { entity ->
                    Insight(
                        id = entity.id,
                        type = mapStringToType(entity.type),
                        message = entity.message,
                        actionRoute = entity.actionRoute,
                        generatedAt = Instant.fromEpochMilliseconds(entity.timestamp)
                    )
                }
            }
            // If no cache, return empty list on parse error or generic failure (never crash)
            return emptyList()
        }
    }

    /** Two decimals is all the precision an insight needs, and it keeps the prompt small. */
    private fun Double.roundedToCents(): String {
        val cents = kotlin.math.round(this * 100) / 100
        return cents.toString()
    }

    private fun mapStringToType(typeStr: String): InsightType {
        return when (typeStr.uppercase()) {
            "SPENDING" -> InsightType.SPENDING
            "SAVING" -> InsightType.SAVING
            "TREND" -> InsightType.TREND
            else -> InsightType.SPENDING
        }
    }

    /**
     * Builds the prompt from **aggregates only** — per-category totals plus the period's income
     * and expense sums. Descriptions, timestamps and ids never leave the device.
     *
     * @param languageTag BCP-47 tag for the app's current language, so the insights come back in
     *   the language the user chose rather than a language hardcoded into the prompt.
     */
    private fun buildPrompt(transactions: List<Transaction>, languageTag: String): String = buildString {
        append(
            "You are a personal finance advisor. Analyse the aggregated spending summary and " +
                "return exactly 3 insights as a JSON array. Each insight has: type " +
                "(SPENDING|SAVING|TREND), message (max 80 chars, written in the language with " +
                "BCP-47 tag '$languageTag'), actionRoute (optional: transactions, budgets, or " +
                "goals). Be specific with the numbers you are given, and encouraging rather " +
                "than judgmental. Do not invent figures that are not in the summary.",
        )
        append("\n\nAggregated summary of the user's last 30 days:\n")

        if (transactions.isEmpty()) {
            append("No transactions in the last 30 days.\n")
            return@buildString
        }

        val income = transactions.filter { it.amount > 0 }.sumOf { it.amount }
        val expenses = transactions.filter { it.amount < 0 }.sumOf { -it.amount }
        append("- Total income: ${income.roundedToCents()}\n")
        append("- Total expenses: ${expenses.roundedToCents()}\n")
        append("- Number of transactions: ${transactions.size}\n")
        append("- Spending by category:\n")

        transactions.filter { it.amount < 0 }
            .groupBy { it.category }
            .mapValues { (_, rows) -> rows.sumOf { -it.amount } }
            .entries
            .sortedByDescending { it.value }
            .forEach { (category, total) ->
                append("  - $category: ${total.roundedToCents()}\n")
            }
    }

    private class RateLimitException : Exception("Rate Limit Exceeded")
    private class ApiException(msg: String) : Exception(msg)
    private class ParseException(msg: String) : Exception(msg)

    private companion object {
        /** Values that look like a key but aren't one; treated the same as "no key". */
        val PLACEHOLDER_KEYS = setOf(
            "MOCK_IOS_API_KEY",
            "MOCK_WASM_API_KEY",
            "YOUR_WASM_API_KEY",
            "YOUR_IOS_API_KEY",
        )
    }
}
