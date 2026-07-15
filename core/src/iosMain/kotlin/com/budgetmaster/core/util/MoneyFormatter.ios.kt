package com.budgetmaster.core.util

import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle

actual object MoneyFormatter {
    actual fun format(amount: Double, currencyCode: String): String {
        val formatter = NSNumberFormatter().apply {
            numberStyle = NSNumberFormatterCurrencyStyle
            this.currencyCode = currencyCode
        }
        return formatter.stringFromNumber(NSNumber(double = amount)) ?: amount.toString()
    }
}
