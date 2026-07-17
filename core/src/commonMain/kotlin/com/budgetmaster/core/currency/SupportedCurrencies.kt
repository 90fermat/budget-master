package com.budgetmaster.core.currency

/**
 * The currency codes the app offers.
 *
 * Single source of truth: this list was duplicated in the Settings picker and the account editor,
 * which meant adding a currency in one place silently left the other behind. The rate fetcher
 * reads it too, so a new code here is automatically fetched.
 */
val SUPPORTED_CURRENCY_CODES = listOf("USD", "EUR", "GBP", "XAF", "CAD", "NGN")
