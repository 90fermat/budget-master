@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
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
import com.budgetmaster.dashboard.presentation.components.AiInsightsWidget
import com.budgetmaster.dashboard.presentation.components.BalanceCard
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Roborazzi screenshot tests for Dashboard UI components (ported from Paparazzi,
 * which has no stable AGP 9-compatible release).
 *
 * Golden images live under `composeApp/src/test/snapshots/`. Record with
 * `gradlew :composeApp:recordRoborazziDebug`, verify with
 * `gradlew :composeApp:verifyRoborazziDebug`.
 */
// Plain Application: BudgetMasterApplication would re-run initKoin() per test and crash.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [35],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    application = android.app.Application::class,
)
class DashboardScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ─── Test fixtures ───────────────────────────────────────────────────────

    private val positiveBalance = BalanceSummary(
        totalBalance = 12_450.80,
        monthlyIncome = 5_320.00,
        monthlyExpenses = 2_869.20,
        balanceTrend = BalanceTrend.POSITIVE,
        trendPercentage = 2.4
    )

    private val negativeBalance = BalanceSummary(
        totalBalance = -1_284.50,
        monthlyIncome = 2_100.00,
        monthlyExpenses = 3_384.50,
        balanceTrend = BalanceTrend.NEGATIVE,
        trendPercentage = -8.7
    )

    private val sampleBudgets = listOf(
        BudgetProgress("1", "Food & Dining", "🍔", 450.0, 500.0, 0.90, BudgetStatus.WARNING),
        BudgetProgress("2", "Entertainment", "🎬", 180.0, 150.0, 1.20, BudgetStatus.EXCEEDED),
        BudgetProgress("3", "Transport", "🚗", 80.0, 200.0, 0.40, BudgetStatus.OK)
    )

    private val sampleTransactions = listOf(
        Transaction("t1", -45.20, "Food", "Starbucks Coffee", 1_750_000_000L),
        Transaction("t2", -120.00, "Transport", "Monthly Bus Pass", 1_750_001_000L),
        Transaction("t3", 5_320.00, "Salary", "June Salary", 1_750_002_000L)
    )

    private val sampleChartData = listOf(
        ChartPoint(date = LocalDate(2026, 6, 1), income = 1_300.0, expenses = 800.0, balance = 500.0),
        ChartPoint(date = LocalDate(2026, 6, 8), income = 1_400.0, expenses = 950.0, balance = 950.0),
        ChartPoint(date = LocalDate(2026, 6, 15), income = 1_250.0, expenses = 700.0, balance = 1_500.0),
        ChartPoint(date = LocalDate(2026, 6, 22), income = 1_370.0, expenses = 419.20, balance = 2_450.80)
    )

    private val threeInsights = listOf(
        Insight(
            id = "i1",
            type = InsightType.SPENDING,
            message = "Your Starbucks Coffee spending has increased by 15% this week. Consider brewing at home to save ~\$45/month.",
            actionRoute = "transactions",
            generatedAt = Clock.System.now()
        ),
        Insight(
            id = "i2",
            type = InsightType.SAVING,
            message = "Great job! You've reached 80% of your holiday savings goal. Keep it up!",
            actionRoute = null,
            generatedAt = Clock.System.now()
        ),
        Insight(
            id = "i3",
            type = InsightType.TREND,
            message = "Based on your 3-month trend, you are on track to save \$1,200 this quarter.",
            actionRoute = "reports",
            generatedAt = Clock.System.now()
        )
    )

    private val loadedState = DashboardState(
        isLoading = false,
        balance = positiveBalance,
        budgets = sampleBudgets,
        topTransactions = sampleTransactions,
        chartData = sampleChartData,
        insights = InsightsState.Success(threeInsights)
    )

    private val errorState = loadedState.copy(
        insights = InsightsState.Success(emptyList()),
        error = "Failed to load latest data. Please check your connection."
    )

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * 1% pixel-change tolerance: animated composables (count-up balance, shimmer,
     * snackbar slide-in) produce sub-frame variance between record and verify runs.
     */
    private val roborazziOptions = RoborazziOptions(
        compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.01f)
    )

    private fun snapshot(name: String, darkTheme: Boolean, content: @Composable () -> Unit) {
        composeRule.setContent {
            AppTheme(palette = AppPalette.INDIGO, darkTheme = darkTheme) {
                content()
            }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/$name.png",
            roborazziOptions = roborazziOptions,
        )
    }

    // ─── DashboardContent ────────────────────────────────────────────────────

    @Test
    fun dashboardContent_loaded_light() = snapshot("dashboardContent_loaded_light", darkTheme = false) {
        DashboardContent(state = loadedState, onIntent = {})
    }

    @Test
    fun dashboardContent_loaded_dark() = snapshot("dashboardContent_loaded_dark", darkTheme = true) {
        DashboardContent(state = loadedState, onIntent = {})
    }

    @Test
    fun dashboardContent_loading_light() = snapshot("dashboardContent_loading_light", darkTheme = false) {
        DashboardContent(state = DashboardState(isLoading = true), onIntent = {})
    }

    @Test
    fun dashboardContent_loading_dark() = snapshot("dashboardContent_loading_dark", darkTheme = true) {
        DashboardContent(state = DashboardState(isLoading = true), onIntent = {})
    }

    @Test
    fun dashboardContent_error_light() = snapshot("dashboardContent_error_light", darkTheme = false) {
        DashboardContent(state = errorState, onIntent = {})
    }

    @Test
    fun dashboardContent_error_dark() = snapshot("dashboardContent_error_dark", darkTheme = true) {
        DashboardContent(state = errorState, onIntent = {})
    }

    // ─── BalanceCard ─────────────────────────────────────────────────────────

    @Test
    fun balanceCard_positive_light() = snapshot("balanceCard_positive_light", darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) { BalanceCard(balanceSummary = positiveBalance) }
    }

    @Test
    fun balanceCard_positive_dark() = snapshot("balanceCard_positive_dark", darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) { BalanceCard(balanceSummary = positiveBalance) }
    }

    @Test
    fun balanceCard_negative_light() = snapshot("balanceCard_negative_light", darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) { BalanceCard(balanceSummary = negativeBalance) }
    }

    @Test
    fun balanceCard_negative_dark() = snapshot("balanceCard_negative_dark", darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) { BalanceCard(balanceSummary = negativeBalance) }
    }

    // ─── AiInsightsWidget ────────────────────────────────────────────────────

    @Test
    fun aiInsightsWidget_loading_light() = snapshot("aiInsightsWidget_loading_light", darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            AiInsightsWidget(
                insightsState = InsightsState.Loading,
                onInsightClicked = {},
                onInsightDismissed = {},
                onRetry = {}
            )
        }
    }

    @Test
    fun aiInsightsWidget_loading_dark() = snapshot("aiInsightsWidget_loading_dark", darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) {
            AiInsightsWidget(
                insightsState = InsightsState.Loading,
                onInsightClicked = {},
                onInsightDismissed = {},
                onRetry = {}
            )
        }
    }

    @Test
    fun aiInsightsWidget_threeInsights_light() = snapshot("aiInsightsWidget_threeInsights_light", darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            AiInsightsWidget(
                insightsState = InsightsState.Success(threeInsights),
                onInsightClicked = {},
                onInsightDismissed = {},
                onRetry = {}
            )
        }
    }

    @Test
    fun aiInsightsWidget_threeInsights_dark() = snapshot("aiInsightsWidget_threeInsights_dark", darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) {
            AiInsightsWidget(
                insightsState = InsightsState.Success(threeInsights),
                onInsightClicked = {},
                onInsightDismissed = {},
                onRetry = {}
            )
        }
    }

    @Test
    fun aiInsightsWidget_error_light() = snapshot("aiInsightsWidget_error_light", darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            AiInsightsWidget(
                insightsState = InsightsState.Error("Could not load AI insights. Check your connection and try again."),
                onInsightClicked = {},
                onInsightDismissed = {},
                onRetry = {}
            )
        }
    }

    @Test
    fun aiInsightsWidget_error_dark() = snapshot("aiInsightsWidget_error_dark", darkTheme = true) {
        Box(modifier = Modifier.padding(16.dp)) {
            AiInsightsWidget(
                insightsState = InsightsState.Error("Could not load AI insights. Check your connection and try again."),
                onInsightClicked = {},
                onInsightDismissed = {},
                onRetry = {}
            )
        }
    }
}
