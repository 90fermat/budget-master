package com.budgetmaster.core.sms

import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.provider_mtn_momo
import budgetmaster.core.generated.resources.provider_orange_money
import org.jetbrains.compose.resources.StringResource

/**
 * The mobile-money providers the app knows by id.
 *
 * The ids are the same strings the parsers return from `MoneyMessageParser.provider` and the keys
 * used in `AppSettings.smsImportAccounts`. Centralised so the id is written once: a typo between a
 * parser's `provider` and a settings key would silently route imports nowhere.
 */
object MoneyProviders {
    const val ORANGE_MONEY = "orange_money"
    const val MTN_MOMO = "mtn_momo"
}

/**
 * The display name for a provider id, or null for one with no catalogued name.
 *
 * Returns a [StringResource] rather than a resolved string so both callers can use it: the
 * importer resolves it with the suspend `getString` at notification-write time, the Settings UI
 * with the composable `stringResource`. A null lets the caller fall back to the raw id rather than
 * show nothing.
 */
fun moneyProviderLabelRes(provider: String): StringResource? = when (provider) {
    MoneyProviders.ORANGE_MONEY -> Res.string.provider_orange_money
    MoneyProviders.MTN_MOMO -> Res.string.provider_mtn_momo
    else -> null
}
