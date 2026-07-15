package com.budgetmaster.core.util

import java.text.NumberFormat
import java.util.Currency

actual object MoneyFormatter {
    actual fun format(amount: Double, currencyCode: String): String {
        val format = NumberFormat.getCurrencyInstance()
        runCatching { format.currency = Currency.getInstance(currencyCode) }
        return format.format(amount)
    }
}
