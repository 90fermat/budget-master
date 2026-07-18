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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.reports_by_category
import budgetmaster.core.generated.resources.reports_no_breakdown
import budgetmaster.core.generated.resources.reports_counterparty_count
import budgetmaster.core.generated.resources.reports_payers
import budgetmaster.core.generated.resources.reports_payees
import budgetmaster.core.generated.resources.reports_counterparties
import budgetmaster.core.generated.resources.reports_fees_subtitle
import budgetmaster.core.generated.resources.reports_fees_title
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
import budgetmaster.core.generated.resources.reports_change_vs_previous
import budgetmaster.core.generated.resources.reports_ai_title
import budgetmaster.core.generated.resources.reports_ai_summarize
import budgetmaster.core.generated.resources.reports_ai_thinking
import budgetmaster.core.generated.resources.reports_ai_ask_label
import budgetmaster.core.generated.resources.reports_ai_ask_placeholder
import budgetmaster.core.generated.resources.reports_ai_ask_action
import budgetmaster.core.generated.resources.reports_ai_failed
import budgetmaster.core.generated.resources.reports_ai_rate_limited
import budgetmaster.core.generated.resources.ai_disclaimer
import com.budgetmaster.core.designsystem.components.GuidanceHost
import com.budgetmaster.core.designsystem.components.HelpIconButton
import com.budgetmaster.core.designsystem.components.rememberGuidance
import com.budgetmaster.core.guidance.GuidanceKey
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.designsystem.financialColors
import com.budgetmaster.core.util.MoneyFormatter
import com.budgetmaster.core.util.formatSignedPercent
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
    val guidance = rememberGuidance(GuidanceKey.REPORTS)
    GuidanceHost(guidance)

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
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    HelpIconButton(onClick = guidance::show)
                }
            }

            Spacer(Modifier.height(Spacing.medium))
            RangeChips(state.range) { viewModel.onIntent(ReportsIntent.RangeChanged(it)) }
            Spacer(Modifier.height(Spacing.medium))

            val report = state.report
            when {
                state.isEmpty -> EmptyState()
                report != null -> {
                    // AI coaching sits above the numbers it talks about, and only when the user
                    // has opted in (state.aiEnabled already folds in provider availability).
                    if (state.aiEnabled && !report.isEmpty) {
                        AiReportSection(
                            state = state,
                            onGenerate = { viewModel.onIntent(ReportsIntent.GenerateNarrative) },
                            onAsk = { viewModel.onIntent(ReportsIntent.AskQuestion(it)) },
                        )
                        Spacer(Modifier.height(Spacing.medium))
                    }
                    ReportBody(report)
                }
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

/**
 * The AI coaching card: a one-tap "summarize this period" narrative plus a free-text Q&A box.
 *
 * Both send only the report aggregates (never raw transactions) and carry the shared "not
 * financial advice" disclaimer, since a confident sentence about someone's money reads as advice.
 */
@Composable
private fun AiReportSection(
    state: ReportsState,
    onGenerate: () -> Unit,
    onAsk: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        Text(
            text = stringResource(Res.string.reports_ai_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // Narrative
        when (val n = state.narrative) {
            AiText.Idle -> OutlinedButton(onClick = onGenerate, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.reports_ai_summarize))
            }
            AiText.Loading -> AiBusyRow(stringResource(Res.string.reports_ai_thinking))
            is AiText.Ready -> AiTextBlock(n.text)
            is AiText.Failed -> AiTextBlock(stringResource(aiFailureRes(n.message)))
        }

        // Q&A
        var question by remember { mutableStateOf("") }
        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            label = { Text(stringResource(Res.string.reports_ai_ask_label)) },
            placeholder = { Text(stringResource(Res.string.reports_ai_ask_placeholder)) },
            singleLine = true,
            enabled = state.answer !is AiText.Loading,
            trailingIcon = {
                if (state.answer is AiText.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = { onAsk(question) }, enabled = question.isNotBlank()) {
                        Text(stringResource(Res.string.reports_ai_ask_action))
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onAsk(question) }),
            modifier = Modifier.fillMaxWidth(),
        )
        when (val a = state.answer) {
            is AiText.Ready -> AiTextBlock(a.text)
            is AiText.Failed -> AiTextBlock(stringResource(aiFailureRes(a.message)))
            else -> Unit
        }

        Text(
            text = stringResource(Res.string.ai_disclaimer),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AiBusyRow(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AiTextBlock(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
}

/** "rate_limited" gets its own copy; everything else is the generic failure. */
private fun aiFailureRes(code: String) =
    if (code == "rate_limited") Res.string.reports_ai_rate_limited else Res.string.reports_ai_failed

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

    if (report.totalFees > 0.0) {
        Spacer(Modifier.height(Spacing.medium))
        FeesCard(report)
    }
    if (report.topPayees.isNotEmpty() || report.topPayers.isNotEmpty()) {
        Spacer(Modifier.height(Spacing.medium))
        CounterpartySection(report)
    }
}

/**
 * What transaction fees cost over the period.
 *
 * Shown only when there are any. Mobile-money charges are a real, recurring cost here and they
 * used to vanish into the totals — no competitor in this market surfaces them.
 */
@Composable
private fun FeesCard(report: ReportSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    stringResource(Res.string.reports_fees_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(Res.string.reports_fees_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                MoneyFormatter.format(report.totalFees, report.currencyCode),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.financialColors.expense,
            )
        }
    }
}

/**
 * Who money goes to and comes from — the question people actually ask, and one only answerable
 * since mobile-money import started capturing counterparty names.
 */
@Composable
private fun CounterpartySection(report: ReportSummary) {
    var showPayers by remember { mutableStateOf(false) }
    val rows = if (showPayers) report.topPayers else report.topPayees

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.large)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(Res.string.reports_counterparties),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    FilterChip(
                        selected = !showPayers,
                        onClick = { showPayers = false },
                        label = { Text(stringResource(Res.string.reports_payees)) },
                    )
                    FilterChip(
                        selected = showPayers,
                        onClick = { showPayers = true },
                        label = { Text(stringResource(Res.string.reports_payers)) },
                    )
                }
            }
            Spacer(Modifier.height(Spacing.small))

            if (rows.isEmpty()) {
                Text(
                    stringResource(Res.string.reports_no_breakdown),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(row.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                        Text(
                            stringResource(Res.string.reports_counterparty_count, row.transactionCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        MoneyFormatter.format(row.amount, report.currencyCode),
                        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        color = if (showPayers) {
                            MaterialTheme.financialColors.income
                        } else {
                            MaterialTheme.financialColors.expense
                        },
                    )
                }
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
                text = stringResource(
                    Res.string.reports_change_vs_previous,
                    formatSignedPercent(change.toDouble() * 100, decimals = 0),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (isGood) MaterialTheme.financialColors.income else MaterialTheme.financialColors.expense,
            )
        }
    }
}

/**
 * Spending or income by category.
 *
 * The same donut serves both: each slice's share is a fraction of its own total, so the two are
 * never mixed. Income was previously invisible — the breakdown only ever covered `amount < 0`, so
 * "where does my money come from" had no answer anywhere in the app.
 */
@Composable
private fun CategorySection(report: ReportSummary) {
    var showIncome by remember { mutableStateOf(false) }
    val slices = if (showIncome) report.incomeCategories else report.categories

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.large)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(Res.string.reports_by_category),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                    FilterChip(
                        selected = !showIncome,
                        onClick = { showIncome = false },
                        label = { Text(stringResource(Res.string.reports_expenses)) },
                    )
                    FilterChip(
                        selected = showIncome,
                        onClick = { showIncome = true },
                        label = { Text(stringResource(Res.string.reports_income)) },
                    )
                }
            }
            Spacer(Modifier.height(Spacing.medium))

            if (slices.isEmpty()) {
                // A period can genuinely have spending but no income (or the reverse); saying so
                // beats an empty circle the user has to interpret.
                Text(
                    text = stringResource(Res.string.reports_no_breakdown),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            // The canvas is opaque to screen readers, so the summary carries the numbers.
            val a11y = stringResource(
                Res.string.reports_chart_a11y,
                slices.take(5).joinToString(", ") {
                    "${it.name} ${(it.share * 100).roundToInt()}%"
                }.ifBlank { "—" },
            )
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CategoryDonut(slices = slices, description = a11y)
            }
            Spacer(Modifier.height(Spacing.medium))
            slices.take(6).forEach { Legend(it, report.currencyCode) }
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
