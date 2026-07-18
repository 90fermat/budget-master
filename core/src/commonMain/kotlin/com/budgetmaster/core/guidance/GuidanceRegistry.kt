package com.budgetmaster.core.guidance

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.guide_accounts_1_body
import budgetmaster.core.generated.resources.guide_accounts_1_title
import budgetmaster.core.generated.resources.guide_accounts_2_body
import budgetmaster.core.generated.resources.guide_accounts_2_title
import budgetmaster.core.generated.resources.guide_accounts_3_body
import budgetmaster.core.generated.resources.guide_accounts_3_title
import budgetmaster.core.generated.resources.guide_accounts_4_body
import budgetmaster.core.generated.resources.guide_accounts_4_title
import budgetmaster.core.generated.resources.guide_accounts_intro
import budgetmaster.core.generated.resources.guide_accounts_title
import budgetmaster.core.generated.resources.guide_budgets_1_body
import budgetmaster.core.generated.resources.guide_budgets_1_title
import budgetmaster.core.generated.resources.guide_budgets_2_body
import budgetmaster.core.generated.resources.guide_budgets_2_title
import budgetmaster.core.generated.resources.guide_budgets_3_body
import budgetmaster.core.generated.resources.guide_budgets_3_title
import budgetmaster.core.generated.resources.guide_budgets_intro
import budgetmaster.core.generated.resources.guide_budgets_title
import budgetmaster.core.generated.resources.guide_dashboard_1_body
import budgetmaster.core.generated.resources.guide_dashboard_1_title
import budgetmaster.core.generated.resources.guide_dashboard_2_body
import budgetmaster.core.generated.resources.guide_dashboard_2_title
import budgetmaster.core.generated.resources.guide_dashboard_3_body
import budgetmaster.core.generated.resources.guide_dashboard_3_title
import budgetmaster.core.generated.resources.guide_dashboard_intro
import budgetmaster.core.generated.resources.guide_dashboard_title
import budgetmaster.core.generated.resources.guide_goals_1_body
import budgetmaster.core.generated.resources.guide_goals_1_title
import budgetmaster.core.generated.resources.guide_goals_2_body
import budgetmaster.core.generated.resources.guide_goals_2_title
import budgetmaster.core.generated.resources.guide_goals_3_body
import budgetmaster.core.generated.resources.guide_goals_3_title
import budgetmaster.core.generated.resources.guide_goals_intro
import budgetmaster.core.generated.resources.guide_goals_title
import budgetmaster.core.generated.resources.guide_reports_1_body
import budgetmaster.core.generated.resources.guide_reports_1_title
import budgetmaster.core.generated.resources.guide_reports_2_body
import budgetmaster.core.generated.resources.guide_reports_2_title
import budgetmaster.core.generated.resources.guide_reports_3_body
import budgetmaster.core.generated.resources.guide_reports_3_title
import budgetmaster.core.generated.resources.guide_reports_intro
import budgetmaster.core.generated.resources.guide_reports_title
import budgetmaster.core.generated.resources.guide_settings_1_body
import budgetmaster.core.generated.resources.guide_settings_1_title
import budgetmaster.core.generated.resources.guide_settings_2_body
import budgetmaster.core.generated.resources.guide_settings_2_title
import budgetmaster.core.generated.resources.guide_settings_3_body
import budgetmaster.core.generated.resources.guide_settings_3_title
import budgetmaster.core.generated.resources.guide_settings_intro
import budgetmaster.core.generated.resources.guide_settings_title
import budgetmaster.core.generated.resources.guide_transactions_1_body
import budgetmaster.core.generated.resources.guide_transactions_1_title
import budgetmaster.core.generated.resources.guide_transactions_2_body
import budgetmaster.core.generated.resources.guide_transactions_2_title
import budgetmaster.core.generated.resources.guide_transactions_3_body
import budgetmaster.core.generated.resources.guide_transactions_3_title
import budgetmaster.core.generated.resources.guide_transactions_4_body
import budgetmaster.core.generated.resources.guide_transactions_4_title
import budgetmaster.core.generated.resources.guide_transactions_intro
import budgetmaster.core.generated.resources.guide_transactions_title

/**
 * Every screen's guide, in one place.
 *
 * Lives in `:core` rather than in each feature so Settings can list them all without
 * depending on seven feature modules, and so a missing guide is a compile-time gap in one
 * file rather than something you notice on the screen that lacks it.
 */
object GuidanceRegistry {

    val guides: Map<GuidanceKey, ScreenGuide> = listOf(
        ScreenGuide(
            key = GuidanceKey.DASHBOARD,
            title = Res.string.guide_dashboard_title,
            intro = Res.string.guide_dashboard_intro,
            notes = listOf(
                FeatureNote(
                    Icons.Filled.AccountBalanceWallet,
                    Res.string.guide_dashboard_1_title,
                    Res.string.guide_dashboard_1_body,
                ),
                FeatureNote(
                    Icons.Filled.Swipe,
                    Res.string.guide_dashboard_2_title,
                    Res.string.guide_dashboard_2_body,
                ),
                FeatureNote(
                    Icons.Filled.AutoAwesome,
                    Res.string.guide_dashboard_3_title,
                    Res.string.guide_dashboard_3_body,
                ),
            ),
        ),
        ScreenGuide(
            key = GuidanceKey.TRANSACTIONS,
            title = Res.string.guide_transactions_title,
            intro = Res.string.guide_transactions_intro,
            notes = listOf(
                FeatureNote(
                    Icons.Filled.Search,
                    Res.string.guide_transactions_1_title,
                    Res.string.guide_transactions_1_body,
                ),
                FeatureNote(
                    Icons.Filled.Swipe,
                    Res.string.guide_transactions_2_title,
                    Res.string.guide_transactions_2_body,
                ),
                FeatureNote(
                    Icons.Filled.AccountBalanceWallet,
                    Res.string.guide_transactions_3_title,
                    Res.string.guide_transactions_3_body,
                ),
                FeatureNote(
                    Icons.Filled.Autorenew,
                    Res.string.guide_transactions_4_title,
                    Res.string.guide_transactions_4_body,
                ),
            ),
        ),
        ScreenGuide(
            key = GuidanceKey.BUDGETS,
            title = Res.string.guide_budgets_title,
            intro = Res.string.guide_budgets_intro,
            notes = listOf(
                FeatureNote(
                    Icons.Filled.Calculate,
                    Res.string.guide_budgets_1_title,
                    Res.string.guide_budgets_1_body,
                ),
                FeatureNote(
                    Icons.Filled.Warning,
                    Res.string.guide_budgets_2_title,
                    Res.string.guide_budgets_2_body,
                ),
                FeatureNote(
                    Icons.Filled.PieChart,
                    Res.string.guide_budgets_3_title,
                    Res.string.guide_budgets_3_body,
                ),
            ),
        ),
        ScreenGuide(
            key = GuidanceKey.GOALS,
            title = Res.string.guide_goals_title,
            intro = Res.string.guide_goals_intro,
            notes = listOf(
                FeatureNote(
                    Icons.Filled.Savings,
                    Res.string.guide_goals_1_title,
                    Res.string.guide_goals_1_body,
                ),
                FeatureNote(
                    Icons.AutoMirrored.Filled.TrendingUp,
                    Res.string.guide_goals_2_title,
                    Res.string.guide_goals_2_body,
                ),
                FeatureNote(
                    Icons.Filled.Edit,
                    Res.string.guide_goals_3_title,
                    Res.string.guide_goals_3_body,
                ),
            ),
        ),
        ScreenGuide(
            key = GuidanceKey.ACCOUNTS,
            title = Res.string.guide_accounts_title,
            intro = Res.string.guide_accounts_intro,
            notes = listOf(
                FeatureNote(
                    Icons.Filled.Tune,
                    Res.string.guide_accounts_1_title,
                    Res.string.guide_accounts_1_body,
                ),
                FeatureNote(
                    Icons.Filled.AccountBalanceWallet,
                    Res.string.guide_accounts_2_title,
                    Res.string.guide_accounts_2_body,
                ),
                FeatureNote(
                    Icons.Filled.SwapHoriz,
                    Res.string.guide_accounts_3_title,
                    Res.string.guide_accounts_3_body,
                ),
                FeatureNote(
                    Icons.Filled.Archive,
                    Res.string.guide_accounts_4_title,
                    Res.string.guide_accounts_4_body,
                ),
            ),
        ),
        ScreenGuide(
            key = GuidanceKey.REPORTS,
            title = Res.string.guide_reports_title,
            intro = Res.string.guide_reports_intro,
            notes = listOf(
                FeatureNote(
                    Icons.AutoMirrored.Filled.CompareArrows,
                    Res.string.guide_reports_1_title,
                    Res.string.guide_reports_1_body,
                ),
                FeatureNote(
                    Icons.Filled.Timeline,
                    Res.string.guide_reports_2_title,
                    Res.string.guide_reports_2_body,
                ),
                FeatureNote(
                    Icons.Filled.FileDownload,
                    Res.string.guide_reports_3_title,
                    Res.string.guide_reports_3_body,
                ),
            ),
        ),
        ScreenGuide(
            key = GuidanceKey.SETTINGS,
            title = Res.string.guide_settings_title,
            intro = Res.string.guide_settings_intro,
            notes = listOf(
                FeatureNote(
                    Icons.Filled.Palette,
                    Res.string.guide_settings_1_title,
                    Res.string.guide_settings_1_body,
                ),
                FeatureNote(
                    Icons.Filled.Translate,
                    Res.string.guide_settings_2_title,
                    Res.string.guide_settings_2_body,
                ),
                FeatureNote(
                    Icons.Filled.BarChart,
                    Res.string.guide_settings_3_title,
                    Res.string.guide_settings_3_body,
                ),
            ),
        ),
    ).associateBy { it.key }

    /**
     * The guide for [key].
     *
     * Non-null by contract — a test asserts every [GuidanceKey] has one, so a new screen
     * fails the build rather than shipping a `?` that opens nothing.
     */
    fun guideFor(key: GuidanceKey): ScreenGuide =
        guides.getValue(key)
}
