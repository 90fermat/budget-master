package com.budgetmaster.core.guidance

import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource

/** The screens that have a guide. One entry per guided screen. */
enum class GuidanceKey {
    DASHBOARD,
    TRANSACTIONS,
    BUDGETS,
    GOALS,
    ACCOUNTS,
    REPORTS,
    SETTINGS,
}

/**
 * One thing a screen can do.
 *
 * Holds [StringResource]s rather than resolved text so the registry stays a plain value —
 * buildable outside composition and localized at render time, following the app's own
 * language setting rather than whatever locale it was built in.
 */
data class FeatureNote(
    val icon: ImageVector,
    val title: StringResource,
    val body: StringResource,
)

/** A screen's guide: what it is, then what you can do on it. */
data class ScreenGuide(
    val key: GuidanceKey,
    val title: StringResource,
    val intro: StringResource,
    val notes: List<FeatureNote>,
)
