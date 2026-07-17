@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.budgetmaster.dashboard.domain.usecase

import com.budgetmaster.core.model.Transaction
import com.budgetmaster.dashboard.domain.model.BalanceSummary
import com.budgetmaster.dashboard.domain.model.BalanceTrend
import com.budgetmaster.dashboard.domain.model.Period
import com.budgetmaster.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import io.kotest.matchers.shouldBe
import io.kotest.matchers.doubles.plusOrMinus
import com.budgetmaster.dashboard.domain.model.ChartPoint
import com.budgetmaster.dashboard.domain.model.BudgetProgress
import com.budgetmaster.dashboard.domain.model.Insight

class GetBalanceSummaryUseCaseTest {

    private class FakeDashboardRepository(
        private val transactions: List<Transaction>,
        private val accountBalances: Double = 0.0
    ) : DashboardRepository {
        
        override fun getBalanceSummary(period: Period): Flow<BalanceSummary> {
            val nowMs = Clock.System.now().toEpochMilliseconds()
            val thirtyDaysAgo = nowMs - 30L * 24 * 60 * 60 * 1000L

            val monthlyIncome = transactions
                .filter { it.timestamp >= thirtyDaysAgo && it.amount > 0 }
                .sumOf { it.amount }

            val monthlyExpenses = transactions
                .filter { it.timestamp >= thirtyDaysAgo && it.amount < 0 }
                .sumOf { kotlin.math.abs(it.amount) }

            val totalBalance = if (accountBalances != 0.0) {
                accountBalances
            } else {
                transactions.sumOf { it.amount }
            }

            val trend = if (monthlyIncome >= monthlyExpenses) BalanceTrend.POSITIVE else BalanceTrend.NEGATIVE
            val trendPercentage = if (monthlyExpenses > 0.0) {
                ((monthlyIncome - monthlyExpenses) / monthlyExpenses * 100.0)
            } else {
                0.0
            }

            return flowOf(
                BalanceSummary(
                    totalBalance = totalBalance,
                    monthlyIncome = monthlyIncome,
                    monthlyExpenses = monthlyExpenses,
                    balanceTrend = trend,
                    trendPercentage = trendPercentage
                )
            )
        }

        override fun getChartData(period: Period): Flow<List<ChartPoint>> = flowOf(emptyList())
        override fun getBudgetProgress(): Flow<List<BudgetProgress>> = flowOf(emptyList())
        override fun getTopTransactions(limit: Int): Flow<List<Transaction>> = flowOf(emptyList())
        override val isAiConfigured: Boolean = true
        override suspend fun getAiInsights(forceRefresh: Boolean, languageTag: String): Result<List<Insight>> =
            Result.success(emptyList())
        override suspend fun deleteTransaction(id: String) {}
        override suspend fun dismissInsight(id: String) {}
    }

    @Test
    fun `correct balance calculation with income and expenses`() = runTest {
        val now = Clock.System.now().toEpochMilliseconds()
        val transactions = listOf(
            Transaction("1", 1000.0, "Salary", "Salary", now),
            Transaction("2", -200.0, "Food", "Dinner", now),
            Transaction("3", -100.0, "Shopping", "Clothes", now)
        )
        val repository = FakeDashboardRepository(transactions)
        val useCase = GetBalanceSummaryUseCase(repository)

        val summary = useCase(Period.MONTH).first()
        summary.totalBalance shouldBe 700.0
        summary.monthlyIncome shouldBe 1000.0
        summary.monthlyExpenses shouldBe 300.0
    }

    @Test
    fun `correct trend percentage calculation`() = runTest {
        val now = Clock.System.now().toEpochMilliseconds()
        val transactions = listOf(
            Transaction("1", 500.0, "Income", "Paycheck", now),
            Transaction("2", -200.0, "Expense", "Groceries", now)
        )
        val repository = FakeDashboardRepository(transactions)
        val useCase = GetBalanceSummaryUseCase(repository)

        val summary = useCase(Period.MONTH).first()
        summary.trendPercentage shouldBe (150.0 plusOrMinus 0.001) // (500 - 200) / 200 * 100 = 150%
        summary.balanceTrend shouldBe BalanceTrend.POSITIVE
    }

    @Test
    fun `empty transactions list returns zero balance and monthly totals`() = runTest {
        val repository = FakeDashboardRepository(emptyList())
        val useCase = GetBalanceSummaryUseCase(repository)

        val summary = useCase(Period.MONTH).first()
        summary.totalBalance shouldBe 0.0
        summary.monthlyIncome shouldBe 0.0
        summary.monthlyExpenses shouldBe 0.0
        summary.trendPercentage shouldBe 0.0
    }
}
