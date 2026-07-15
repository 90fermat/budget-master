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
     */
    fun format(amount: Double, currencyCode: String): String
}

/**
 * Formats the absolute value of [amount] with an explicit leading `+` or `-`,
 * for transaction rows where inflow/outflow direction is emphasized.
 */
fun MoneyFormatter.formatSigned(amount: Double, currencyCode: String): String {
    val sign = if (amount < 0) "-" else "+"
    return sign + format(kotlin.math.abs(amount), currencyCode)
}
