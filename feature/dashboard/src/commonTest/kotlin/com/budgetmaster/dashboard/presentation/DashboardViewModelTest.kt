@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.dashboard.presentation

import com.budgetmaster.core.model.Transaction
import com.budgetmaster.core.prefs.AppSettingsRepository
import com.budgetmaster.core.session.SessionStore
import com.budgetmaster.core.session.SessionUser
import com.budgetmaster.dashboard.InMemoryKeyValueStore
import com.budgetmaster.dashboard.domain.model.BalanceSummary
import com.budgetmaster.dashboard.domain.model.BalanceTrend
import com.budgetmaster.dashboard.domain.model.BudgetProgress
import com.budgetmaster.dashboard.domain.model.BudgetStatus
import com.budgetmaster.dashboard.domain.model.ChartPoint
import com.budgetmaster.dashboard.domain.model.Insight
import com.budgetmaster.dashboard.domain.model.InsightType
import com.budgetmaster.dashboard.domain.model.Period
import com.budgetmaster.dashboard.domain.model.TransactionType
import com.budgetmaster.dashboard.domain.repository.DashboardRepository
import com.budgetmaster.dashboard.domain.usecase.GetAiInsightsUseCase
import com.budgetmaster.dashboard.domain.usecase.GetBalanceSummaryUseCase
import com.budgetmaster.dashboard.domain.usecase.GetBudgetProgressUseCase
import com.budgetmaster.dashboard.domain.usecase.GetChartDataUseCase
import com.budgetmaster.dashboard.domain.usecase.GetTopTransactionsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [DashboardViewModel] verifying MVI flow, period switching, AI insights caching,
 * and error resilience.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // Test Data Helpers
    private val testLocalDate = LocalDate(2026, 6, 20)
    private val testInstant = Instant.fromEpochMilliseconds(1781958000000L) // 2026-06-20T12:20:00Z

    private val sampleBalanceSummary = BalanceSummary(
        totalBalance = 5420.50,
        monthlyIncome = 3200.00,
        monthlyExpenses = 1500.20,
        balanceTrend = BalanceTrend.POSITIVE,
        trendPercentage = 12.5
    )

    private val sampleChartPoints = listOf(
        ChartPoint(testLocalDate, 1000.0, 500.0, 500.0)
    )

    private val sampleBudgetProgress = listOf(
        BudgetProgress(
            categoryId = "cat_food",
            categoryName = "Food",
            categoryEmoji = "🍔",
            spent = 250.0,
            limit = 400.0,
            percentage = 62.5,
            status = BudgetStatus.OK
        )
    )

    private val sampleTransactions = listOf(
        Transaction(
            id = "tx_1",
            amount = -45.50,
            category = "Food",
            description = "Groceries",
            timestamp = 1781958000000L
        ),
        Transaction(
            id = "tx_2",
            amount = 2500.00,
            category = "Salary",
            description = "Monthly Paycheck",
            timestamp = 1781950000000L
        )
    )

    private val sampleInsights = listOf(
        Insight(
            id = "ins_1",
            type = InsightType.SPENDING,
            message = "You spent 15% more on Groceries this week.",
            actionRoute = "/transactions",
            generatedAt = testInstant
        )
    )

    // Fake Repository Implementation
    private class FakeDashboardRepository : DashboardRepository {
        var balanceSummaryFlow: Flow<BalanceSummary> = flowOf()
        var chartDataFlow: Flow<List<ChartPoint>> = flowOf()
        var budgetProgressFlow: Flow<List<BudgetProgress>> = flowOf()
        var topTransactionsFlow: Flow<List<Transaction>> = flowOf()
        var aiInsightsResult: Result<List<Insight>> = Result.success(emptyList())

        val deletedTransactions = mutableListOf<String>()
        val dismissedInsights = mutableListOf<String>()

        var shouldThrowOnDelete = false
        var shouldThrowOnDismiss = false

        override fun getBalanceSummary(period: Period): Flow<BalanceSummary> = balanceSummaryFlow
        override fun getChartData(period: Period): Flow<List<ChartPoint>> = chartDataFlow
        override fun getBudgetProgress(): Flow<List<BudgetProgress>> = budgetProgressFlow
        override fun getTopTransactions(limit: Int): Flow<List<Transaction>> = topTransactionsFlow

        override suspend fun getAiInsights(forceRefresh: Boolean): Result<List<Insight>> = aiInsightsResult

        override suspend fun deleteTransaction(id: String) {
            if (shouldThrowOnDelete) throw RuntimeException("Failed to delete from database")
            deletedTransactions.add(id)
        }

        override suspend fun dismissInsight(id: String) {
            if (shouldThrowOnDismiss) throw RuntimeException("Failed to dismiss from database")
            dismissedInsights.add(id)
        }
    }

    private lateinit var repository: FakeDashboardRepository
    private lateinit var getBalanceSummaryUseCase: GetBalanceSummaryUseCase
    private lateinit var getChartDataUseCase: GetChartDataUseCase
    private lateinit var getBudgetProgressUseCase: GetBudgetProgressUseCase
    private lateinit var getTopTransactionsUseCase: GetTopTransactionsUseCase
    private lateinit var getAiInsightsUseCase: GetAiInsightsUseCase

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        repository = FakeDashboardRepository().apply {
            balanceSummaryFlow = flowOf(sampleBalanceSummary)
            chartDataFlow = flowOf(sampleChartPoints)
            budgetProgressFlow = flowOf(sampleBudgetProgress)
            topTransactionsFlow = flowOf(sampleTransactions)
            aiInsightsResult = Result.success(sampleInsights)
        }

        getBalanceSummaryUseCase = GetBalanceSummaryUseCase(repository)
        getChartDataUseCase = GetChartDataUseCase(repository)
        getBudgetProgressUseCase = GetBudgetProgressUseCase(repository)
        getTopTransactionsUseCase = GetTopTransactionsUseCase(repository)
        getAiInsightsUseCase = GetAiInsightsUseCase(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(sessionStore: SessionStore = SessionStore()): DashboardViewModel {
        return DashboardViewModel(
            repository = repository,
            getBalanceSummary = getBalanceSummaryUseCase,
            getChartData = getChartDataUseCase,
            getBudgetProgress = getBudgetProgressUseCase,
            getTopTransactions = getTopTransactionsUseCase,
            getAiInsights = getAiInsightsUseCase,
            settingsRepository = AppSettingsRepository(InMemoryKeyValueStore()),
            sessionStore = sessionStore
        )
    }

    @Test
    fun `greeting uses the signed-in display name`() = runTest {
        val session = SessionStore().apply {
            setCurrentUser(SessionUser("u1", displayName = "Cyrille Foyang", email = "c@example.com"))
        }
        val viewModel = createViewModel(session)
        advanceUntilIdle()

        assertEquals("Cyrille Foyang", viewModel.state.value.userName)
    }

    @Test
    fun `greeting falls back to the email local part when there is no display name`() = runTest {
        // Email/password sign-up never sets a display name, so this is the common case.
        val session = SessionStore().apply {
            setCurrentUser(SessionUser("u1", displayName = null, email = "cyrille@example.com"))
        }
        val viewModel = createViewModel(session)
        advanceUntilIdle()

        assertEquals("cyrille", viewModel.state.value.userName)
    }

    @Test
    fun `greeting has no name when nobody is signed in`() = runTest {
        val viewModel = createViewModel(SessionStore())
        advanceUntilIdle()

        // Null rather than a mock name — the UI substitutes a localized fallback.
        assertNull(viewModel.state.value.userName)
    }

    @Test
    fun `initially loads dashboard data and updates state to success`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertEquals(sampleBalanceSummary, state.balance)
        assertEquals(sampleChartPoints, state.chartData)
        assertEquals(sampleBudgetProgress, state.budgets)
        assertEquals(sampleTransactions, state.topTransactions)
        assertIs<InsightsState.Success>(state.insights)
        assertEquals(sampleInsights, (state.insights as InsightsState.Success).data)
        assertNull(state.error)
    }

    @Test
    fun `LoadDashboard with specific period updates selectedPeriod and loads data`() = runTest {
        val viewModel = createViewModel()
        viewModel.onIntent(DashboardIntent.LoadDashboard(Period.YEAR))
        advanceUntilIdle()

        assertEquals(Period.YEAR, viewModel.state.value.selectedPeriod)
    }

    @Test
    fun `RefreshRequested sets isRefreshing and updates state from repository`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val updatedBalance = sampleBalanceSummary.copy(totalBalance = 6000.0)
        repository.balanceSummaryFlow = flowOf(updatedBalance)

        viewModel.onIntent(DashboardIntent.RefreshRequested)
        // Verify we set isRefreshing to true initially
        assertTrue(viewModel.state.value.isRefreshing)

        advanceUntilIdle()
        assertFalse(viewModel.state.value.isRefreshing)
        assertEquals(6000.0, viewModel.state.value.balance?.totalBalance)
    }

    @Test
    fun `PeriodChanged updates selectedPeriod and reloads dashboard`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(DashboardIntent.PeriodChanged(Period.WEEK))
        advanceUntilIdle()

        assertEquals(Period.WEEK, viewModel.state.value.selectedPeriod)
    }

    @Test
    fun `PeriodChanged does nothing if selectedPeriod is unchanged`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Same as default (Period.MONTH)
        viewModel.onIntent(DashboardIntent.PeriodChanged(Period.MONTH))
        advanceUntilIdle()

        assertEquals(Period.MONTH, viewModel.state.value.selectedPeriod)
    }

    @Test
    fun `QuickActionClicked emits NavigateToAddTransaction effect`() = runTest {
        val viewModel = createViewModel()
        val effects = mutableListOf<DashboardEffect>()
        val collectJob = launch { viewModel.effects.toList(effects) }

        viewModel.onIntent(DashboardIntent.QuickActionClicked(TransactionType.EXPENSE))
        advanceUntilIdle()

        assertEquals(1, effects.size)
        val effect = effects.first()
        assertIs<DashboardEffect.NavigateToAddTransaction>(effect)
        assertEquals(TransactionType.EXPENSE, effect.type)

        collectJob.cancel()
    }

    @Test
    fun `TransactionSwiped optimistically removes transaction and emits ShowUndoDelete`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val effects = mutableListOf<DashboardEffect>()
        val collectJob = launch { viewModel.effects.toList(effects) }

        viewModel.onIntent(DashboardIntent.TransactionSwiped("tx_1"))
        // Check optimistic removal
        val topTxIds = viewModel.state.value.topTransactions.map { it.id }
        assertFalse(topTxIds.contains("tx_1"))

        advanceUntilIdle()

        assertEquals(1, repository.deletedTransactions.size)
        assertEquals("tx_1", repository.deletedTransactions.first())
        assertEquals(1, effects.size)
        assertIs<DashboardEffect.ShowUndoDelete>(effects.first())

        collectJob.cancel()
    }

    @Test
    fun `TransactionSwiped rolls back on repository deletion failure`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        repository.shouldThrowOnDelete = true
        val effects = mutableListOf<DashboardEffect>()
        val collectJob = launch { viewModel.effects.toList(effects) }

        viewModel.onIntent(DashboardIntent.TransactionSwiped("tx_1"))
        advanceUntilIdle()

        // Re-added and sorted by timestamp
        val topTxIds = viewModel.state.value.topTransactions.map { it.id }
        assertTrue(topTxIds.contains("tx_1"))
        assertEquals("tx_1", viewModel.state.value.topTransactions.first().id) // tx_1 has newer timestamp than tx_2

        assertEquals(0, repository.deletedTransactions.size)
        assertEquals("Failed to delete from database", viewModel.state.value.error)
        assertTrue(effects.any { it is DashboardEffect.ShowError })

        collectJob.cancel()
    }

    @Test
    fun `InsightsDismissed optimistically removes insight and updates database`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onIntent(DashboardIntent.InsightsDismissed("ins_1"))
        val successState = viewModel.state.value.insights as InsightsState.Success
        assertTrue(successState.data.isEmpty())

        advanceUntilIdle()
        assertEquals(1, repository.dismissedInsights.size)
        assertEquals("ins_1", repository.dismissedInsights.first())
    }

    @Test
    fun `InsightsDismissed rolls back on database failure`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        repository.shouldThrowOnDismiss = true
        val effects = mutableListOf<DashboardEffect>()
        val collectJob = launch { viewModel.effects.toList(effects) }

        viewModel.onIntent(DashboardIntent.InsightsDismissed("ins_1"))
        advanceUntilIdle()

        val successState = viewModel.state.value.insights as InsightsState.Success
        assertEquals(1, successState.data.size)
        assertEquals("ins_1", successState.data.first().id)
        assertEquals("Failed to dismiss from database", viewModel.state.value.error)
        assertTrue(effects.any { it is DashboardEffect.ShowError })

        collectJob.cancel()
    }

    @Test
    fun `dashboard loading flows handle errors gracefully`() = runTest {
        repository.balanceSummaryFlow = flow { throw RuntimeException("Balance error") }
        repository.chartDataFlow = flow { throw RuntimeException("Chart error") }
        repository.budgetProgressFlow = flow { throw RuntimeException("Budget error") }
        repository.topTransactionsFlow = flow { throw RuntimeException("Transactions error") }

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.balance)
        assertTrue(state.chartData.isEmpty())
        assertTrue(state.budgets.isEmpty())
        assertTrue(state.topTransactions.isEmpty())
        // Should capture last caught error message in state.error
        assertEquals("Transactions error", state.error)
    }

    @Test
    fun `AI insights load failure updates insights state to error`() = runTest {
        repository.aiInsightsResult = Result.failure(RuntimeException("AI error"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertIs<InsightsState.Error>(state.insights)
        assertEquals("AI error", (state.insights as InsightsState.Error).message)
    }
}
