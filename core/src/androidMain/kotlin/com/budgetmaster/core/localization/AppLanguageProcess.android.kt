package com.budgetmaster.core.localization

import java.util.Locale

/**
 * Sets the JVM default locale, which is what compose-resources reads when resolving a string
 * outside composition.
 *
 * Deliberately process-wide rather than scoped to the call: the alternative is threading a
 * resource environment through every use case that might one day produce user-facing text, and the
 * one that forgets is found by a user reading a notification in a language they do not use.
 */
actual fun applyAppLanguageToProcess(tag: String?) {
    if (tag == null) return
    Locale.setDefault(Locale.forLanguageTag(tag))
}
