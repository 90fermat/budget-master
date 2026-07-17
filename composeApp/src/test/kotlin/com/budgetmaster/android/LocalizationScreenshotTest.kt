@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.LayoutDirection
import com.budgetmaster.core.designsystem.AppPalette
import com.budgetmaster.core.designsystem.AppTheme
import com.budgetmaster.core.model.Transaction
import com.budgetmaster.dashboard.domain.model.BalanceSummary
import com.budgetmaster.dashboard.domain.model.BalanceTrend
import com.budgetmaster.dashboard.domain.model.BudgetProgress
import com.budgetmaster.dashboard.domain.model.BudgetStatus
import com.budgetmaster.dashboard.domain.model.ChartPoint
import com.budgetmaster.dashboard.domain.model.Insight
import com.budgetmaster.dashboard.domain.model.InsightType
import com.budgetmaster.dashboard.presentation.DashboardContent
import com.budgetmaster.dashboard.presentation.DashboardState
import com.budgetmaster.dashboard.presentation.InsightsState
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage

import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale
import kotlin.time.Clock

/**
 * Localization and layout-direction screenshots — the Phase 5 truncation and RTL passes.
 *
 * **Why French rather than a pseudo-locale.** Android's `en-XA` pseudo-locale inflates strings at
 * the *framework* resource layer; this app's strings come from compose-resources, which would
 * simply fall back to English and prove nothing. French is the real second locale, averages ~20%
 * longer than English, and is what users actually see — so it is the honest truncation test.
 *
 * **Why RTL matters with no RTL language shipped.** EN and FR are both LTR, so nothing here is
 * user-visible today. Rendering mirrored is what catches hardcoded left/right padding *before*
 * an RTL language is added, when the cost of finding it is a screenshot rather than a rewrite.
 *
 * Golden images live under `composeApp/src/test/snapshots/`. Record with
 * `gradlew :composeApp:recordRoborazziDebug`, verify with `gradlew :composeApp:verifyRoborazziDebug`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [35],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    application = android.app.Application::class,
)
class LocalizationScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val defaultLocale: Locale = Locale.getDefault()

    @After
    fun restoreLocale() {
        // Locale.setDefault leaks across tests in the same JVM otherwise.
        Locale.setDefault(defaultLocale)
    }

    private val balance = BalanceSummary(
        totalBalance = 12_450.80,
        monthlyIncome = 5_320.00,
        monthlyExpenses = 2_869.20,
        balanceTrend = BalanceTrend.POSITIVE,
        trendPercentage = 2.4,
    )

    private val budgets = listOf(
        BudgetProgress("cat_food", "Food & Dining", "🍔", 450.0, 500.0, 0.90, BudgetStatus.WARNING),
        BudgetProgress("cat_entertainment", "Entertainment", "🎬", 180.0, 150.0, 1.20, BudgetStatus.EXCEEDED),
        BudgetProgress("cat_transport", "Transport", "🚗", 80.0, 200.0, 0.40, BudgetStatus.OK),
    )

    private val transactions = listOf(
        Transaction("t1", -45.20, "cat_food", "Starbucks Coffee", 1_750_000_000L),
        Transaction("t2", -120.00, "cat_transport", "Monthly Bus Pass", 1_750_001_000L),
        Transaction("t3", 5_320.00, "cat_salary", "June Salary", 1_750_002_000L),
    )

    /**
     * Deliberately empty.
     *
     * This test is about text, and the Android chart is Vico, which populates through a coroutine
     * — Robolectric captures whichever state it happens to be in, so a chart with data made the
     * golden flaky (it rendered on one run and was blank on the next, with no code change).
     * Empty also exercises the localized "no data" string, which is a string this test should
     * cover anyway.
     */
    private val chartData = emptyList<ChartPoint>()

    /** The longest copy the widget can hold, which is where clipping shows up first. */
    private val insights = listOf(
        Insight(
            id = "i1",
            type = InsightType.SPENDING,
            message = "Vos dépenses en restaurants ont augmenté de 15 % ce mois-ci par rapport au mois dernier.",
            actionRoute = "transactions",
            generatedAt = Clock.System.now(),
        ),
        Insight(
            id = "i2",
            type = InsightType.SAVING,
            message = "Bravo ! Vous avez atteint 80 % de votre objectif d'épargne pour les vacances.",
            actionRoute = null,
            generatedAt = Clock.System.now(),
        ),
    )

    private val loadedState = DashboardState(
        isLoading = false,
        balance = balance,
        budgets = budgets,
        topTransactions = transactions,
        chartData = chartData,
        insights = InsightsState.Success(insights),
        userName = "Cyrille Foyang",
    )

    private val roborazziOptions = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f),
    )

    /**
     * @param locale drives both the Robolectric qualifier and the JVM default, because
     *   compose-resources resolves its locale from `Locale.getDefault()` rather than the Android
     *   configuration.
     */
    private fun snapshot(
        name: String,
        locale: Locale,
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        darkTheme: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        RuntimeEnvironment.setQualifiers("+${locale.language}")
        Locale.setDefault(locale)

        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                AppTheme(palette = AppPalette.INDIGO, darkTheme = darkTheme) {
                    content()
                }
            }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/$name.png",
            roborazziOptions = roborazziOptions,
        )
    }

    // ─── French: the truncation pass ─────────────────────────────────────────

    @Test
    fun dashboard_french_light() =
        snapshot("dashboard_french_light", Locale.FRENCH) {
            DashboardContent(state = loadedState, onIntent = {})
        }

    @Test
    fun dashboard_french_dark() =
        snapshot("dashboard_french_dark", Locale.FRENCH, darkTheme = true) {
            DashboardContent(state = loadedState, onIntent = {})
        }

    /** English side by side, so a French-only clip is obvious rather than a judgement call. */
    @Test
    fun dashboard_english_light() =
        snapshot("dashboard_english_light", Locale.ENGLISH) {
            DashboardContent(state = loadedState, onIntent = {})
        }

    // ─── RTL: the smoke test ─────────────────────────────────────────────────

    @Test
    fun dashboard_rtl_light() =
        snapshot("dashboard_rtl_light", Locale.ENGLISH, layoutDirection = LayoutDirection.Rtl) {
            DashboardContent(state = loadedState, onIntent = {})
        }

    @Test
    fun dashboard_rtl_french() =
        snapshot("dashboard_rtl_french", Locale.FRENCH, layoutDirection = LayoutDirection.Rtl) {
            DashboardContent(state = loadedState, onIntent = {})
        }
}
