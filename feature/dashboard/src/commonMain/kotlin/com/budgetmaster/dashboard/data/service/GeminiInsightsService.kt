@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.dashboard.data.service

import app.cash.sqldelight.async.coroutines.awaitAsList
import com.budgetmaster.core.ai.GenAiClient
import com.budgetmaster.core.ai.GenAiException
import com.budgetmaster.core.ai.GenAiSchema
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.model.Transaction
import com.budgetmaster.dashboard.data.remote.model.GeminiInsightDto
import com.budgetmaster.dashboard.domain.model.Insight
import com.budgetmaster.dashboard.domain.model.InsightType
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Turns a user's spending into insights, via whatever model provider [GenAiClient] wraps.
 *
 * Caches results for 24 h in SQLDelight and never crashes the dashboard: any failure falls back
 * to the cache, or to nothing.
 *
 * **Only aggregates leave the device.** The prompt carries per-category totals and the period's
 * income/expense sums — never descriptions (free text, and users put names in them), timestamps,
 * or ids. Sending the raw ledger to a third party is not something a finance app should do, and
 * the aggregates are what the model actually reasons about anyway.
 *
 * Two behaviours worth keeping: without a provider this returns nothing rather than the
 * hardcoded "mock" insights it used to invent ("coffee spending up 15%") and present as real
 * analysis; and the request goes through Firebase AI Logic, so no API key ships in the app.
 */
class GeminiInsightsService(
    private val databaseProvider: DatabaseProvider,
    private val genAiClient: GenAiClient,
    /** Waits between rate-limit retries. Tests override it so they don't actually sleep. */
    private val retryBackoffMs: List<Long> = RETRY_BACKOFF_MS,
) {
    private val jsonConfiguration = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    /**
     * Whether a provider is wired on this platform. False on iOS and Web until their SDKs are
     * bridged. Callers should hide the AI surface entirely rather than show an empty section.
     */
    val isConfigured: Boolean
        get() = genAiClient.isAvailable

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

        // 2. Generate
        try {
            if (!isConfigured) {
                // No provider: answer with the cache if we have one, otherwise nothing at all.
                // Never invent insights — a fabricated "your coffee spending rose 15%" is worse
                // than silence in an app whose whole job is telling the truth about money.
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
            val responseText = requestInsightsWithBackoff(prompt)

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
            // Deliberately not logged. This path handles a failure while generating insights from
            // the user's spending, and an exception message here can carry prompt or response
            // fragments - which on Android means the user's finances in logcat, readable by
            // anything with log access on a rooted device and captured by bug reports. The
            // fallback below is the real handling; a println was never part of it.
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

    /**
     * Generates, retrying a rate limit with exponential backoff.
     *
     * The free tier rate-limits per minute, so being rate-limited usually means "wait", not "give
     * up" — the previous behaviour fell straight back to a stale cache on the first one. Only
     * [GenAiException.RateLimited] is retried; every other failure will fail identically however
     * long we wait.
     */
    private suspend fun requestInsightsWithBackoff(prompt: String): String {
        var lastRateLimit: GenAiException.RateLimited? = null

        // One initial attempt plus one per backoff step.
        repeat(retryBackoffMs.size + 1) { attempt ->
            if (attempt > 0) delay(retryBackoffMs[attempt - 1])
            try {
                return genAiClient.generateJson(prompt, INSIGHTS_SCHEMA)
            } catch (e: GenAiException.RateLimited) {
                lastRateLimit = e
            }
        }
        throw lastRateLimit ?: GenAiException.RateLimited()
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
        /**
         * The exact shape [GeminiInsightDto] parses, enforced by the provider rather than
         * requested in prose. `actionRoute` is an enum so the model cannot invent a screen that
         * does not exist, and is optional because not every insight leads somewhere.
         */
        val INSIGHTS_SCHEMA = GenAiSchema.Arr(
            items = GenAiSchema.Obj(
                properties = mapOf(
                    "type" to GenAiSchema.Enumeration(listOf("SPENDING", "SAVING", "TREND")),
                    "message" to GenAiSchema.Str(description = "At most 80 characters."),
                    "actionRoute" to GenAiSchema.Enumeration(listOf("transactions", "budgets", "goals")),
                ),
                optional = listOf("actionRoute"),
            ),
        )

        /** Free-tier rate limits are common and transient; these are the waits between retries. */
        val RETRY_BACKOFF_MS = listOf(1_000L, 4_000L)
    }
}
