package com.budgetmaster.core.localization

/**
 * Applies the user's chosen language to this process, for text resolved outside composition.
 *
 * [LocalAppLocale] handles the UI, but it only takes effect while something is composing. Work that
 * runs without any UI — an SMS arriving while the app is closed, a background pass raising a budget
 * alert — resolves its strings against the process default instead, which is the *device* language.
 * Someone whose phone is in English and whose app is in French was getting English notifications
 * about their own money.
 *
 * @param tag an IETF language tag, or null to follow the device.
 */
expect fun applyAppLanguageToProcess(tag: String?)
