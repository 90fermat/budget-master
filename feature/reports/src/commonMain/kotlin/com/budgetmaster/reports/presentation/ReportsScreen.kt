@file:OptIn(ExperimentalLayoutApi::class)

package com.budgetmaster.reports.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.reports_by_category
import budgetmaster.core.generated.resources.reports_chart_a11y
import budgetmaster.core.generated.resources.reports_empty
import budgetmaster.core.generated.resources.reports_expenses
import budgetmaster.core.generated.resources.reports_export_csv
import budgetmaster.core.generated.resources.reports_export_started
import budgetmaster.core.generated.resources.reports_export_unavailable
import budgetmaster.core.generated.resources.reports_income
import budgetmaster.core.generated.resources.reports_net
import budgetmaster.core.generated.resources.reports_range_all
import budgetmaster.core.generated.resources.reports_range_month
import budgetmaster.core.generated.resources.reports_range_quarter
import budgetmaster.core.generated.resources.reports_range_year
import budgetmaster.core.generated.resources.reports_title
import budgetmaster.core.generated.resources.reports_transfers_note
import budgetmaster.core.generated.resources.reports_trend
import budgetmaster.core.generated.resources.reports_trend_a11y
import budgetmaster.core.generated.resources.reports_vs_previous
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.designsystem.financialColors
import com.budgetmaster.core.util.MoneyFormatter
import com.budgetmaster.reports.domain.model.CategorySlice
import com.budgetmaster.reports.domain.model.ReportRange
import com.budgetmaster.reports.domain.model.ReportSummary
import com.budgetmaster.reports.presentation.components.CategoryDonut
import com.budgetmaster.reports.presentation.components.TrendChart
import com.budgetmaster.core.designsystem.parseHexColor
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/**
 * Reports: period totals with a comparison to the previous period, spending by category, and
 * an income/expense trend — all scoped to the active wallet and excluding transfers.
 */
@Composable
fun ReportsScreen(viewModel: ReportsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportStarted = stringResource(Res.string.reports_export_started)
    val exportUnavailable = stringResource(Res.string.reports_export_unavailable)
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ReportsEffect.ExportStarted -> snackbarHostState.showSnackbar(exportStarted)
                ReportsEffect.ExportUnavailable -> snackbarHostState.showSnackbar(exportUnavailable)
                is ReportsEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.medium),
        ) {
            Spacer(Modifier.height(Spacing.medium))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(Res.string.reports_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                OutlinedButton(
                    onClick = { viewModel.onIntent(ReportsIntent.ExportCsvClicked) },
                    enabled = !state.isExporting && !state.isEmpty,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (state.isExporting) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(Res.string.reports_export_csv))
                    }
                }
            }

            Spacer(Modifier.height(Spacing.medium))
            RangeChips(state.range) { viewModel.onIntent(ReportsIntent.RangeChanged(it)) }
            Spacer(Modifier.height(Spacing.medium))

            val report = state.report
            when {
                state.isEmpty -> EmptyState()
                report != null -> ReportBody(report)
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun RangeChips(selected: ReportRange, onSelect: (ReportRange) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
        ReportRange.entries.forEach { range ->
            FilterChip(
                selected = selected == range,
                onClick = { onSelect(range) },
                label = { Text(range.label()) },
            )
        }
    }
}

@Composable
private fun ReportRange.label(): String = stringResource(
    when (this) {
        ReportRange.MONTH -> Res.string.reports_range_month
        ReportRange.QUARTER -> Res.string.reports_range_quarter
        ReportRange.YEAR -> Res.string.reports_range_year
        ReportRange.ALL -> Res.string.reports_range_all
    },
)

@Composable
private fun ReportBody(report: ReportSummary) {
    SummaryCard(report)
    Spacer(Modifier.height(Spacing.medium))

    // Side-by-side on wide layouts, stacked on phones.
    BoxWithConstraints {
        if (maxWidth >= 600.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                Box(Modifier.weight(1f)) { CategorySection(report) }
                Box(Modifier.weight(1f)) { TrendSection(report) }
            }
        } else {
            Column {
                CategorySection(report)
                Spacer(Modifier.height(Spacing.medium))
                TrendSection(report)
            }
        }
    }
}

@Composable
private fun SummaryCard(report: ReportSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(Spacing.large), verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            Text(
                stringResource(Res.string.reports_net),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                MoneyFormatter.format(report.net, report.currencyCode),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.large)) {
                Metric(
                    label = stringResource(Res.string.reports_income),
                    value = MoneyFormatter.format(report.totalIncome, report.currencyCode),
                    change = report.incomeChange,
                    positiveIsGood = true,
                )
                Metric(
                    label = stringResource(Res.string.reports_expenses),
                    value = MoneyFormatter.format(report.totalExpenses, report.currencyCode),
                    change = report.expenseChange,
                    positiveIsGood = false,
                )
            }
            Text(
                stringResource(Res.string.reports_transfers_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun Metric(label: String, value: String, change: Float?, positiveIsGood: Boolean) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        if (change != null) {
            // Rising income is good; rising spend is not — colour by meaning, not by sign.
            val isGood = if (positiveIsGood) change >= 0 else change <= 0
            Text(
                text = "${if (change >= 0) "+" else ""}${(change * 100).roundToInt()}% " +
                    stringResource(Res.string.reports_vs_previous),
                style = MaterialTheme.typography.bodySmall,
                color = if (isGood) MaterialTheme.financialColors.income else MaterialTheme.financialColors.expense,
            )
        }
    }
}

@Composable
private fun CategorySection(report: ReportSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.large)) {
            Text(
                stringResource(Res.string.reports_by_category),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(Spacing.medium))

            // The canvas is opaque to screen readers, so the summary carries the numbers.
            val a11y = stringResource(
                Res.string.reports_chart_a11y,
                report.categories.take(5).joinToString(", ") {
                    "${it.name} ${(it.share * 100).roundToInt()}%"
                }.ifBlank { "—" },
            )
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CategoryDonut(slices = report.categories, description = a11y)
            }
            Spacer(Modifier.height(Spacing.medium))
            report.categories.take(6).forEach { Legend(it, report.currencyCode) }
        }
    }
}

@Composable
private fun Legend(slice: CategorySlice, currencyCode: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(parseHexColor(slice.colorHex, MaterialTheme.colorScheme.primary)),
        )
        Spacer(Modifier.width(Spacing.small))
        Text(slice.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            "${(slice.share * 100).roundToInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(Spacing.small))
        Text(
            MoneyFormatter.format(slice.amount, currencyCode),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun TrendSection(report: ReportSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.large)) {
            Text(
                stringResource(Res.string.reports_trend),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(Spacing.medium))
            val a11y = stringResource(
                Res.string.reports_trend_a11y,
                MoneyFormatter.format(report.totalIncome, report.currencyCode),
                MoneyFormatter.format(report.totalExpenses, report.currencyCode),
                report.trend.size,
            )
            TrendChart(points = report.trend, description = a11y)
            Spacer(Modifier.height(Spacing.small))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                LegendKey(stringResource(Res.string.reports_income), MaterialTheme.financialColors.income)
                LegendKey(stringResource(Res.string.reports_expenses), MaterialTheme.financialColors.expense)
            }
        }
    }
}

@Composable
private fun LegendKey(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
        Text(
            stringResource(Res.string.reports_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
