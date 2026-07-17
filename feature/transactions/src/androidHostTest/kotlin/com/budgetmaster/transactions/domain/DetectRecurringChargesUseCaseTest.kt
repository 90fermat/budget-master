package com.budgetmaster.transactions.domain

import com.budgetmaster.transactions.domain.model.TransactionCategory
import com.budgetmaster.transactions.domain.model.TransactionItem
import com.budgetmaster.transactions.domain.usecase.DetectRecurringChargesUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DetectRecurringChargesUseCaseTest {

    private val useCase = DetectRecurringChargesUseCase()

    // 15th of each month in early 2026, as epoch millis (UTC-ish; exact tz doesn't matter here).
    private fun month(m: Int) = 1_736_000_000_000L + (m - 1) * 30L * 24 * 60 * 60 * 1000L

    private fun expense(desc: String, amount: Double, timestamp: Long) = TransactionItem(
        id = "$desc-$timestamp",
        amount = -amount,
        description = desc,
        timestamp = timestamp,
        category = TransactionCategory("cat_other", "Other", "📦", "#94A3B8"),
        notes = null,
    )

    @Test
    fun `flags a charge that repeats across months at a stable amount`() {
        val txns = listOf(
            expense("Netflix", 15.99, month(1)),
            expense("netflix ", 15.99, month(2)), // case/space variations still group
            expense("NETFLIX", 15.99, month(3)),
        )
        val result = useCase(txns)

        assertEquals(1, result.size)
        assertEquals("NETFLIX", result[0].description)
        assertEquals(3, result[0].occurrences)
        assertEquals(15.99, result[0].typicalAmount)
    }

    @Test
    fun `ignores a merchant seen many times in a single month`() {
        // Three coffees in one week is frequency, not a subscription.
        val txns = listOf(
            expense("Coffee", 4.5, month(1)),
            expense("Coffee", 4.5, month(1) + 86_400_000L),
            expense("Coffee", 4.5, month(1) + 2 * 86_400_000L),
        )
        assertTrue(useCase(txns).isEmpty(), "same-month repeats are one occurrence")
    }

    @Test
    fun `ignores a merchant whose amount swings too much`() {
        // Same shop each month, wildly different spend — not a fixed subscription.
        val txns = listOf(
            expense("Groceries", 40.0, month(1)),
            expense("Groceries", 120.0, month(2)),
            expense("Groceries", 30.0, month(3)),
        )
        assertTrue(useCase(txns).isEmpty(), "inconsistent amounts are not a recurring charge")
    }

    @Test
    fun `ignores income even if it repeats`() {
        val salary = List(3) { i ->
            TransactionItem("sal$i", 2000.0, "Payroll", month(i + 1), null, null)
        }
        assertTrue(useCase(salary).isEmpty(), "only expenses can be recurring charges")
    }

    @Test
    fun `tolerates small price changes within tolerance`() {
        // A subscription that nudged from 9.99 to 10.49 is still the same subscription.
        val txns = listOf(
            expense("Spotify", 9.99, month(1)),
            expense("Spotify", 10.49, month(2)),
        )
        assertEquals(1, useCase(txns).size)
    }

    @Test
    fun `sorts the most frequent charges first`() {
        val txns = listOf(
            expense("Gym", 30.0, month(1)),
            expense("Gym", 30.0, month(2)),
            expense("Netflix", 15.0, month(1)),
            expense("Netflix", 15.0, month(2)),
            expense("Netflix", 15.0, month(3)),
        )
        val result = useCase(txns)
        assertEquals(listOf("Netflix", "Gym"), result.map { it.description })
    }
}
