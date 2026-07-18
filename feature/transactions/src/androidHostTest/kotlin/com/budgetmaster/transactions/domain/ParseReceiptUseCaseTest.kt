@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.transactions.domain

import com.budgetmaster.core.ai.GenAiClient
import com.budgetmaster.core.ai.GenAiException
import com.budgetmaster.core.ai.GenAiSchema
import com.budgetmaster.core.ocr.ReceiptImage
import com.budgetmaster.core.ocr.ReceiptTextRecognizer
import com.budgetmaster.core.util.DateUtils
import com.budgetmaster.transactions.domain.model.TransactionCategory
import com.budgetmaster.transactions.domain.usecase.ParseReceiptUseCase
import com.budgetmaster.transactions.domain.usecase.ReceiptScanError
import com.budgetmaster.transactions.domain.usecase.ReceiptScanException
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeReceiptRecognizer(
    override val isAvailable: Boolean = true,
    private val text: String? = "TOTAL 42.50",
) : ReceiptTextRecognizer {
    override suspend fun recognizeText(image: ReceiptImage): String? = text
}

private class FakeReceiptAiClient(
    override val isAvailable: Boolean = true,
    private val respond: () -> String = { "{}" },
) : GenAiClient {
    var lastPrompt: String? = null
    override suspend fun generateJson(prompt: String, schema: GenAiSchema): String {
        lastPrompt = prompt
        return respond()
    }
}

class ParseReceiptUseCaseTest {

    private val categories = listOf(
        TransactionCategory("cat_food", "Food & Dining", "🍔", "#F59E0B"),
        TransactionCategory("cat_groceries", "Groceries", "🛒", "#10B981"),
    )
    private val image = ReceiptImage(byteArrayOf(1, 2, 3))
    private val now = 1_752_753_600_000L

    @Test
    fun `parses total merchant and category from receipt text`() = runBlocking {
        val client = FakeReceiptAiClient {
            """{"total":"42.50","merchant":"Corner Market","categoryId":"cat_groceries","daysAgo":"0"}"""
        }
        val draft = ParseReceiptUseCase(FakeReceiptRecognizer(), client)
            .invoke(image, categories, now).getOrThrow()

        assertEquals(42.50, draft.amountAbs)
        assertEquals("Corner Market", draft.description)
        assertEquals("cat_groceries", draft.categoryId)
        // A receipt is a purchase; defaulting to income on a misread would inflate the balance.
        assertTrue(draft.isExpense)
    }

    @Test
    fun `sends the OCR text to the model, not the image`() = runBlocking {
        val client = FakeReceiptAiClient { """{"total":"1"}""" }
        ParseReceiptUseCase(FakeReceiptRecognizer(text = "MEGA MART\nTOTAL 9.99"), client)
            .invoke(image, categories, now)

        val prompt = client.lastPrompt.orEmpty()
        assertTrue(prompt.contains("MEGA MART"), "the recognised text is what gets summarised")
        assertTrue(prompt.contains("cat_food"), "the category list is included")
    }

    @Test
    fun `resolves daysAgo to the start of that day`() = runBlocking {
        val client = FakeReceiptAiClient { """{"total":"10","daysAgo":"2"}""" }
        val draft = ParseReceiptUseCase(FakeReceiptRecognizer(), client)
            .invoke(image, categories, now).getOrThrow()

        val expected = DateUtils.toLocalDate(now)
            .plus(-2, DateTimeUnit.DAY)
            .atStartOfDayIn(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
        assertEquals(expected, draft.timestamp)
    }

    @Test
    fun `unreadable photo is a typed NoTextFound failure`() = runBlocking {
        val client = FakeReceiptAiClient { error("must not run without text") }
        val result = ParseReceiptUseCase(FakeReceiptRecognizer(text = null), client)
            .invoke(image, categories, now)

        assertEquals(ReceiptScanError.NoTextFound, (result.exceptionOrNull() as ReceiptScanException).error)
    }

    @Test
    fun `text without a total is a typed NoAmount failure`() = runBlocking {
        val result = ParseReceiptUseCase(FakeReceiptRecognizer(), FakeReceiptAiClient { """{"merchant":"X"}""" })
            .invoke(image, categories, now)

        assertEquals(ReceiptScanError.NoAmount, (result.exceptionOrNull() as ReceiptScanException).error)
    }

    @Test
    fun `unavailable when OCR is missing, even if AI is present`() = runBlocking {
        val useCase = ParseReceiptUseCase(FakeReceiptRecognizer(isAvailable = false), FakeReceiptAiClient())
        assertEquals(false, useCase.isAvailable)
        assertEquals(
            ReceiptScanError.Unavailable,
            (useCase.invoke(image, categories, now).exceptionOrNull() as ReceiptScanException).error,
        )
    }

    @Test
    fun `a hallucinated category is dropped rather than applied`() = runBlocking {
        val client = FakeReceiptAiClient { """{"total":"5","categoryId":"cat_not_real"}""" }
        val draft = ParseReceiptUseCase(FakeReceiptRecognizer(), client)
            .invoke(image, categories, now).getOrThrow()

        assertNull(draft.categoryId)
    }

    @Test
    fun `a provider failure is a typed Failed result rather than a throw`() = runBlocking {
        val client = object : GenAiClient {
            override val isAvailable = true
            override suspend fun generateJson(prompt: String, schema: GenAiSchema): String =
                throw GenAiException.RateLimited()
        }
        val result = ParseReceiptUseCase(FakeReceiptRecognizer(), client).invoke(image, categories, now)

        assertEquals(ReceiptScanError.Failed, (result.exceptionOrNull() as ReceiptScanException).error)
    }
}
