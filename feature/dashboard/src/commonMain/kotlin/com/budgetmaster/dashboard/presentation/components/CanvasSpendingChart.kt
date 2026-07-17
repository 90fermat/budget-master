package com.budgetmaster.dashboard.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.chart_no_data
import budgetmaster.core.generated.resources.chart_series_balance
import budgetmaster.core.generated.resources.chart_series_cash_flow
import budgetmaster.core.generated.resources.chart_title
import com.budgetmaster.dashboard.domain.model.ChartPoint
import org.jetbrains.compose.resources.stringResource

/**
 * The cash-flow chart drawn with Compose Canvas.
 *
 * Shared by the platforms that cannot use Vico, which is an Android-only library: without this,
 * iOS and web would each need their own copy of the same drawing code.
 *
 * @param chartData The cash-flow data points to plot.
 * @param modifier The modifier to be applied to the layout.
 */
@Composable
internal fun CanvasSpendingChart(
    chartData: List<ChartPoint>,
    modifier: Modifier = Modifier,
) {
    var isLineChart by remember { mutableStateOf(true) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(24.dp),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.chart_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isLineChart,
                        onClick = { isLineChart = true },
                        label = { Text(stringResource(Res.string.chart_series_balance)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                    FilterChip(
                        selected = !isLineChart,
                        onClick = { isLineChart = false },
                        label = { Text(stringResource(Res.string.chart_series_cash_flow)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            ) {
                if (chartData.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.chart_no_data),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        )
                    }
                    return@Box
                }

                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary
                // outlineVariant is the token for subtle rules like gridlines; a faded
                // outline was reinventing it at a contrast nobody could see.
                val outlineColor = MaterialTheme.colorScheme.outlineVariant

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val paddingBottom = 24.dp.toPx()
                    val paddingLeft = 12.dp.toPx()
                    val paddingRight = 12.dp.toPx()
                    val paddingTop = 12.dp.toPx()

                    val chartWidth = size.width - paddingLeft - paddingRight
                    val chartHeight = size.height - paddingTop - paddingBottom

                    val gridCount = 3
                    for (i in 0 until gridCount) {
                        val y = paddingTop + (chartHeight / (gridCount - 1)) * i
                        drawLine(
                            color = outlineColor,
                            start = Offset(paddingLeft, y),
                            end = Offset(size.width - paddingRight, y),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }

                    if (isLineChart) {
                        val maxVal = chartData.maxOf { it.balance }.coerceAtLeast(1.0)
                        val minVal = chartData.minOf { it.balance }.coerceAtMost(0.0)
                        val range = maxVal - minVal

                        // A single point has no segment to interpolate across, so pin it to the
                        // left edge rather than dividing by a zero-width span.
                        val step = if (chartData.size > 1) chartWidth / (chartData.size - 1) else 0f
                        val points = chartData.mapIndexed { index, point ->
                            Offset(
                                x = paddingLeft + step * index,
                                y = paddingTop + chartHeight -
                                    ((point.balance - minVal) / range * chartHeight).toFloat(),
                            )
                        }

                        val path = Path().apply {
                            points.forEachIndexed { index, offset ->
                                if (index == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                            }
                        }

                        val areaPath = Path().apply {
                            addPath(path)
                            lineTo(points.last().x, paddingTop + chartHeight)
                            lineTo(points.first().x, paddingTop + chartHeight)
                            close()
                        }
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent),
                                startY = points.minOf { it.y },
                                endY = paddingTop + chartHeight,
                            ),
                        )

                        drawPath(
                            path = path,
                            color = primaryColor,
                            style = Stroke(width = 3.dp.toPx()),
                        )

                        points.forEach { point ->
                            drawCircle(color = primaryColor, radius = 5.dp.toPx(), center = point)
                            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = point)
                        }
                    } else {
                        val maxVal = chartData.maxOf { maxOf(it.income, it.expenses) }.coerceAtLeast(1.0)
                        val groupWidth = chartWidth / chartData.size
                        val barWidth = (groupWidth * 0.3f).coerceAtLeast(4f)
                        val spacing = groupWidth * 0.1f

                        chartData.forEachIndexed { index, point ->
                            val xGroup = paddingLeft + groupWidth * index + groupWidth / 2f

                            val incomeHeight = (point.income / maxVal * chartHeight).toFloat()
                            if (incomeHeight > 0) {
                                drawRoundRect(
                                    color = primaryColor,
                                    topLeft = Offset(
                                        xGroup - barWidth - spacing / 2,
                                        paddingTop + chartHeight - incomeHeight,
                                    ),
                                    size = Size(barWidth, incomeHeight),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                )
                            }

                            val expenseHeight = (point.expenses / maxVal * chartHeight).toFloat()
                            if (expenseHeight > 0) {
                                drawRoundRect(
                                    color = secondaryColor,
                                    topLeft = Offset(
                                        xGroup + spacing / 2,
                                        paddingTop + chartHeight - expenseHeight,
                                    ),
                                    size = Size(barWidth, expenseHeight),
                                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
