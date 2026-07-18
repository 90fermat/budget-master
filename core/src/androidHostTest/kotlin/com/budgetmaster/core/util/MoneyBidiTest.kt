package com.budgetmaster.core.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the bidirectional isolation around money and percentages.
 *
 * The Phase 5 RTL screenshot pass found `+2.4%` rendering as `2.4%+` and formatted amounts
 * scrambling outright: a money string is digits, punctuation and a symbol, all of which the
 * Unicode bidi algorithm treats as *neutral*, so an RTL paragraph reorders them by their
 * surroundings. Isolation fixes the display without changing the value — and being invisible
 * control characters, it is exactly the sort of fix that gets deleted by accident.
 */
class MoneyBidiTest {

    private val fsi = '⁨'
    private val pdi = '⁩'

    @Test
    fun `a signed amount is wrapped in an isolate`() {
        val text = MoneyFormatter.formatSigned(-12.5, "USD")

        assertEquals(fsi, text.first(), "must open with FIRST STRONG ISOLATE")
        assertEquals(pdi, text.last(), "must close with POP DIRECTIONAL ISOLATE")
    }

    @Test
    fun `isolation does not alter the visible characters`() {
        val text = MoneyFormatter.formatSigned(-12.5, "USD")
        val visible = text.trim(fsi, pdi)

        assertTrue(visible.contains("12"), "the amount survives: $visible")
        assertTrue(visible.startsWith("-"), "the sign survives: $visible")
        // Nothing else was inserted — only the two wrapping characters.
        assertEquals(text.length, visible.length + 2)
    }

    @Test
    fun `signed percent carries its sign and an isolate`() {
        val positive = formatSignedPercent(2.4).trim(fsi, pdi)
        val negative = formatSignedPercent(-8.7).trim(fsi, pdi)

        assertTrue(positive.startsWith("+"), "expected a leading plus, got $positive")
        assertTrue(positive.endsWith("%"), "expected a trailing percent, got $positive")
        // A negative value already carries its minus; a second sign would read as nonsense.
        assertTrue(negative.startsWith("-"), "expected the value's own minus, got $negative")
        assertTrue(!negative.startsWith("+-"), "must not double up signs: $negative")
    }

    @Test
    fun `percent isolation wraps the whole run, sign included`() {
        val text = formatSignedPercent(2.4)

        assertEquals(fsi, text.first())
        assertEquals(pdi, text.last())
        // The sign must be *inside* the isolate, or it is exactly the character that gets moved.
        assertTrue(text.drop(1).startsWith("+"), "sign belongs inside the isolate: $text")
    }

    @Test
    fun `zero decimals rounds rather than showing a decimal point`() {
        assertEquals("+2%", formatSignedPercent(2.4, decimals = 0).trim(fsi, pdi))
        assertEquals("+3%", formatSignedPercent(2.6, decimals = 0).trim(fsi, pdi))
    }
}
