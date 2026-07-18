package com.budgetmaster.core.util

/**
 * Locale- and currency-aware money formatting.
 *
 * Backed per platform by the native formatter (Android `NumberFormat`,
 * iOS `NSNumberFormatter`, Web `Intl.NumberFormat`) so grouping separators,
 * decimal marks, and symbol placement match the user's device locale.
 */
expect object MoneyFormatter {
    /**
     * Formats [amount] in the given ISO-4217 [currencyCode] (e.g. "USD", "EUR").
     * The value is formatted with its own sign (negative amounts show a minus).
     *
     * The result is **bidi-isolated** by every implementation — see [isolated]. This is a display
     * formatter and there were already a dozen call sites, so isolating here is the only way a
     * new one cannot silently reintroduce the reordering bug. Anything machine-readable (the CSV
     * export) uses the raw `Double` instead and is unaffected.
     */
    fun format(amount: Double, currencyCode: String): String
}

/**
 * Formats the absolute value of [amount] with an explicit leading `+` or `-`,
 * for transaction rows where inflow/outflow direction is emphasized.
 */
fun MoneyFormatter.formatSigned(amount: Double, currencyCode: String): String {
    val sign = if (amount < 0) "-" else "+"
    // format() already isolates, but that would leave the sign *outside* the isolate — and the
    // sign is exactly the character RTL reordering moves. Strip the inner pair and isolate once
    // around the whole run instead of nesting.
    val magnitude = format(kotlin.math.abs(amount), currencyCode).stripIsolates()
    return (sign + magnitude).isolated()
}

/** Unicode FIRST STRONG ISOLATE — opens a run whose direction is decided by its own content. */
private const val FSI = '⁨'

/** POP DIRECTIONAL ISOLATE — closes the innermost isolate. */
private const val PDI = '⁩'

/**
 * Wraps a formatted value so surrounding text cannot reorder it.
 *
 * A money string is a run of digits, punctuation and a symbol — characters the Unicode
 * bidirectional algorithm treats as *neutral*, so in an RTL paragraph they get reordered by their
 * surroundings: `+2.4%` renders as `2.4%+`, and `450,00 $US sur 500,00 $US` scrambles outright.
 * Isolating the run fixes the display without touching the value, and is a no-op in LTR text.
 *
 * Applied at the formatter so every caller is covered rather than each remembering to.
 */
fun String.isolated(): String = "$FSI$this$PDI"

/** Removes isolation characters, so a run can be re-wrapped without nesting. */
fun String.stripIsolates(): String = filterNot { it == FSI || it == PDI }

/**
 * A percentage with an explicit sign, isolated for the same reason amounts are: `+2.4%` is all
 * neutral characters, so an RTL paragraph renders it `2.4%+`.
 *
 * @param percent already scaled to percent (2.4 means 2.4%), not a 0..1 fraction.
 */
fun formatSignedPercent(percent: Double, decimals: Int = 1): String {
    val rounded = if (decimals == 0) {
        kotlin.math.round(percent).toInt().toString()
    } else {
        val factor = 10.0
        ((kotlin.math.round(percent * factor)) / factor).toString()
    }
    val sign = if (percent < 0) "" else "+" // a negative value already carries its minus
    return "$sign$rounded%".isolated()
}
