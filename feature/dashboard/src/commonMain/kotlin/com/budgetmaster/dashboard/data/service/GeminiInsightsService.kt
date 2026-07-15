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
     * Retrieves insights either from local cache (if age < 24h) or generates them via Gemini.
     * Fallbacks to cache in case of rate-limiting or network issues.
     */
    suspend fun getInsights(
        transactions: List<Transaction>,
        forceRefresh: Boolean
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
            if (apiKey.isBlank() || apiKey == "MOCK_IOS_API_KEY" || apiKey == "MOCK_WASM_API_KEY" || apiKey == "YOUR_WASM_API_KEY" || apiKey == "YOUR_IOS_API_KEY") {
                // If API Key is missing or mock, return cached as fallback or some mock values to avoid failure
                println("WARNING: Gemini API Key is missing or a mock. Returning cached insights if available.")
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
                return getMockInsights()
            }

            val systemPrompt = "You are a personal finance advisor. Analyze the spending data and return exactly 3 insights as JSON array. Each insight has: type (SPENDING|SAVING|TREND), message (max 80 chars, French), actionRoute (optional screen route to navigate to). Be specific with numbers. Be encouraging, not judgmental."
            val prompt = buildString {
                append(systemPrompt)
                append("\n\nHere is the personal transaction data of the user for the last 30 days:\n")
                if (transactions.isEmpty()) {
                    append("No transactions in the last 30 days.\n")
                } else {
                    transactions.forEach { tx ->
                        append("- Amount: ${tx.amount}, Category: ${tx.category}, Description: ${tx.description}, Timestamp: ${tx.timestamp}\n")
                    }
                }
            }

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

    private fun mapStringToType(typeStr: String): InsightType {
        return when (typeStr.uppercase()) {
            "SPENDING" -> InsightType.SPENDING
            "SAVING" -> InsightType.SAVING
            "TREND" -> InsightType.TREND
            else -> InsightType.SPENDING
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun getMockInsights(): List<Insight> {
        val now = Clock.System.now()
        return listOf(
            Insight(
                id = "mock_1",
                type = InsightType.SPENDING,
                message = "Vos dépenses en cafés ont augmenté de 15% ce mois-ci. Réduisez-les de 10€.",
                actionRoute = "transactions",
                generatedAt = now
            ),
            Insight(
                id = "mock_2",
                type = InsightType.SAVING,
                message = "Bravo! Vous avez économisé 80% de votre objectif vacances ce mois-ci.",
                actionRoute = "budgets",
                generatedAt = now
            ),
            Insight(
                id = "mock_3",
                type = InsightType.TREND,
                message = "Votre solde net est positif ce mois-ci, une tendance très encourageante.",
                actionRoute = null,
                generatedAt = now
            )
        )
    }

    private class RateLimitException : Exception("Rate Limit Exceeded")
    private class ApiException(msg: String) : Exception(msg)
    private class ParseException(msg: String) : Exception(msg)
}
