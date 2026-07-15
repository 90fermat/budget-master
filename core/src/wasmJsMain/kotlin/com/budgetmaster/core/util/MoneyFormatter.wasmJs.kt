package com.budgetmaster.core.util

actual object MoneyFormatter {
    actual fun format(amount: Double, currencyCode: String): String =
        formatCurrencyIntl(amount, currencyCode)
}

// Kotlin/Wasm requires js(...) to be the entire body of a top-level function.
private fun formatCurrencyIntl(amount: Double, currencyCode: String): String =
    js("new Intl.NumberFormat(undefined, { style: 'currency', currency: currencyCode }).format(amount)")
