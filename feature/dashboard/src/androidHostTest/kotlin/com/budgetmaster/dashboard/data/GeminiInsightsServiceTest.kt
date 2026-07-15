@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.dashboard.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.budgetmaster.core.db.BudgetMasterDatabase
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.model.Transaction
import com.budgetmaster.dashboard.data.service.GeminiInsightsService
import com.budgetmaster.dashboard.domain.model.Insight
import com.budgetmaster.dashboard.domain.model.InsightType
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.HttpRequestData
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlinx.serialization.json.Json
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeminiInsightsServiceTest {

    private lateinit var database: BudgetMasterDatabase
    private lateinit var databaseProvider: DatabaseProvider

    @BeforeTest
    fun setUp() {
        database = TestDatabaseHelper.createInMemoryDatabase()
        databaseProvider = DatabaseProvider(database)
    }

    @Test
    fun testGetInsightsReturnsMockIfApiKeyIsMock() = runBlocking {
        val mockHttpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respond("", HttpStatusCode.OK)
                }
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val service = GeminiInsightsService(
            databaseProvider = databaseProvider,
            httpClient = mockHttpClient,
            apiKeyProvider = { "" }
        )
        val result = service.getInsights(emptyList(), forceRefresh = false)
        assertEquals(3, result.size)
        assertEquals(InsightType.SPENDING, result[0].type)
        assertEquals(InsightType.SAVING, result[1].type)
        assertEquals(InsightType.TREND, result[2].type)
    }

    @Test
    fun testGetInsightsReturnsCachedIfFresh() = runBlocking {
        val queries = database.budgetMasterDatabaseQueries
        val now = Clock.System.now().toEpochMilliseconds()
        queries.insertInsight(
            id = "insight_cached",
            type = "SAVING",
            message = "Cached saving message",
            actionRoute = "budgets",
            timestamp = now
        )

        val mockHttpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    error("Should not hit network since cache is fresh")
                }
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val service = GeminiInsightsService(
            databaseProvider = databaseProvider,
            httpClient = mockHttpClient,
            apiKeyProvider = { "valid_api_key" }
        )
        
        val result = service.getInsights(emptyList(), forceRefresh = false)
        assertEquals(1, result.size)
        assertEquals("insight_cached", result[0].id)
        assertEquals("Cached saving message", result[0].message)
    }

    @Test
    fun testGetInsightsCallsApiAndCaches() = runBlocking {
        val responseBody = """
            {
                "candidates": [
                    {
                        "content": {
                            "parts": [
                                {
                                    "text": "[{\"type\":\"SPENDING\",\"message\":\"High spending.\",\"actionRoute\":\"transactions\"},{\"type\":\"SAVING\",\"message\":\"Saved 50€.\",\"actionRoute\":null},{\"type\":\"TREND\",\"message\":\"Positive trend.\",\"actionRoute\":null}]"
                                }
                            ]
                        }
                    }
                ]
            }
        """.trimIndent()

        var capturedRequest: HttpRequestData? = null
        val mockHttpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    capturedRequest = request
                    respond(
                        content = responseBody,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val service = GeminiInsightsService(
            databaseProvider = databaseProvider,
            httpClient = mockHttpClient,
            apiKeyProvider = { "valid_api_key" }
        )

        val result = service.getInsights(emptyList(), forceRefresh = true)
        
        // Assert request details
        val req = capturedRequest
        assertTrue(req != null)
        assertTrue(req.url.toString().contains("v1beta/models/gemini-2.0-flash:generateContent"))
        assertEquals("valid_api_key", req.url.parameters["key"])

        assertEquals(3, result.size)
        assertEquals(InsightType.SPENDING, result[0].type)
        assertEquals("High spending.", result[0].message)
        assertEquals("transactions", result[0].actionRoute)

        assertEquals(InsightType.SAVING, result[1].type)
        assertEquals("Saved 50€.", result[1].message)
        assertEquals(null, result[1].actionRoute)

        // Verify it was written to cache
        val queries = database.budgetMasterDatabaseQueries
        val cachedEntities = queries.selectAllInsights().executeAsList()
        assertEquals(3, cachedEntities.size)
    }

    @Test
    fun testGetInsightsFallbackToCachedOn429RateLimit() = runBlocking {
        val queries = database.budgetMasterDatabaseQueries
        val now = Clock.System.now().toEpochMilliseconds()
        queries.insertInsight(
            id = "cached_insight_id",
            type = "TREND",
            message = "Trend cached",
            actionRoute = null,
            timestamp = now
        )

        val mockHttpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respond(
                        content = "Too Many Requests",
                        status = HttpStatusCode.TooManyRequests,
                        headers = headersOf(HttpHeaders.ContentType, "text/plain")
                    )
                }
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val service = GeminiInsightsService(
            databaseProvider = databaseProvider,
            httpClient = mockHttpClient,
            apiKeyProvider = { "valid_api_key" }
        )

        val result = service.getInsights(emptyList(), forceRefresh = true)
        assertEquals(1, result.size)
        assertEquals("cached_insight_id", result[0].id)
        assertEquals("Trend cached", result[0].message)
    }

    @Test
    fun testGetInsightsReturnsEmptyOnParseErrorNoCrash() = runBlocking {
        // Bad JSON returned from Gemini API
        val responseBody = """
            {
                "candidates": [
                    {
                        "content": {
                            "parts": [
                                {
                                    "text": "Invalid JSON or not matching structure"
                                }
                            ]
                        }
                    }
                ]
            }
        """.trimIndent()

        val mockHttpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respond(
                        content = responseBody,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val service = GeminiInsightsService(
            databaseProvider = databaseProvider,
            httpClient = mockHttpClient,
            apiKeyProvider = { "valid_api_key" }
        )

        val result = service.getInsights(emptyList(), forceRefresh = true)
        assertTrue(result.isEmpty())
    }
}
