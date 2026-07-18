package com.budgetmaster.core.util

actual object MoneyFormatter {
    // Isolated here so no display call site can forget; see the expect declaration.
    actual fun format(amount: Double, currencyCode: String): String =
        formatCurrencyIntl(amount, currencyCode).isolated()
}

// Kotlin/Wasm requires js(...) to be the entire body of a top-level function.
private fun formatCurrencyIntl(amount: Double, currencyCode: String): String =
    js("new Intl.NumberFormat(undefined, { style: 'currency', currency: currencyCode }).format(amount)")
