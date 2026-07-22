package com.budgetmaster.core.localization

/**
 * No-op for now.
 *
 * iOS has no background entry point that produces text yet — SMS import is Android-only — so there
 * is nothing here that resolves strings outside composition, and [LocalAppLocale] already covers
 * the UI. This exists so the shared code has one call site rather than a platform check.
 */
actual fun applyAppLanguageToProcess(tag: String?) = Unit
