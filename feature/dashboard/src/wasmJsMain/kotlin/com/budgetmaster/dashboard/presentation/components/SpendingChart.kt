package com.budgetmaster.dashboard.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.budgetmaster.dashboard.domain.model.ChartPoint
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun SpendingChart(
    chartData: List<ChartPoint>,
    modifier: Modifier,
    chartAnimation: ChartAnimation
) {
    var isLineChart by remember { mutableStateOf(true) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Analytics (Web)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isLineChart,
                        onClick = { isLineChart = true },
                        label = { Text("Balance") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    FilterChip(
                        selected = !isLineChart,
                        onClick = { isLineChart = false },
                        label = { Text("Cash Flow") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (chartData.isNotEmpty()) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val secondaryColor = MaterialTheme.colorScheme.secondary
                    // outlineVariant is the token for subtle rules like gridlines; a faded
                    // outline was reinventing it at a contrast nobody could see.
                    val outlineColor = MaterialTheme.colorScheme.outlineVariant

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        
                        val paddingBottom = 24.dp.toPx()
                        val paddingLeft = 12.dp.toPx()
                        val paddingRight = 12.dp.toPx()
                        val paddingTop = 12.dp.toPx()
                        
                        val chartWidth = width - paddingLeft - paddingRight
                        val chartHeight = height - paddingTop - paddingBottom
                        
                        // Draw grid lines (3 horizontal lines)
                        val gridCount = 3
                        for (i in 0 until gridCount) {
                            val y = paddingTop + (chartHeight / (gridCount - 1)) * i
                            drawLine(
                                color = outlineColor,
                                start = Offset(paddingLeft, y),
                                end = Offset(width - paddingRight, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        if (isLineChart) {
                            // LINE CHART: Balance
                            val maxVal = chartData.maxOf { it.balance }.coerceAtLeast(1.0)
                            val minVal = chartData.minOf { it.balance }.coerceAtMost(0.0)
                            val range = maxVal - minVal
                            
                            val points = chartData.mapIndexed { index, point ->
                                val x = paddingLeft + (chartWidth / (chartData.size - 1)) * index
                                val y = paddingTop + chartHeight - ((point.balance - minVal) / range * chartHeight).toFloat()
                                Offset(x, y)
                            }
                            
                            // Draw connecting line path
                            val path = Path().apply {
                                points.forEachIndexed { index, offset ->
                                    if (index == 0) {
                                        moveTo(offset.x, offset.y)
                                    } else {
                                        lineTo(offset.x, offset.y)
                                    }
                                }
                            }
                            
                            // Draw gradient area under the path
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
                                    startY = points.map { it.y }.minOrNull() ?: paddingTop,
                                    endY = paddingTop + chartHeight
                                )
                            )
                            
                            // Draw the line itself
                            drawPath(
                                path = path,
                                color = primaryColor,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            
                            // Draw dots at data points
                            points.forEach { point ->
                                drawCircle(
                                    color = primaryColor,
                                    radius = 5.dp.toPx(),
                                    center = point
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.dp.toPx(),
                                    center = point
                                )
                            }
                        } else {
                            // COLUMN CHART: Income and Expense
                            val maxVal = chartData.maxOf { maxOf(it.income, it.expenses) }.coerceAtLeast(1.0)
                            val columnCount = chartData.size
                            val groupWidth = chartWidth / columnCount
                            val barWidth = (groupWidth * 0.3f).coerceAtLeast(4f)
                            val spacing = groupWidth * 0.1f
                            
                            chartData.forEachIndexed { index, point ->
                                val xGroup = paddingLeft + groupWidth * index + groupWidth / 2f
                                
                                // Income bar (Left)
                                val incomeHeight = (point.income / maxVal * chartHeight).toFloat()
                                val incomeX = xGroup - barWidth - spacing / 2
                                val incomeY = paddingTop + chartHeight - incomeHeight
                                if (incomeHeight > 0) {
                                    drawRoundRect(
                                        color = primaryColor,
                                        topLeft = Offset(incomeX, incomeY),
                                        size = Size(barWidth, incomeHeight),
                                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                }
                                
                                // Expense bar (Right)
                                val expenseHeight = (point.expenses / maxVal * chartHeight).toFloat()
                                val expenseX = xGroup + spacing / 2
                                val expenseY = paddingTop + chartHeight - expenseHeight
                                if (expenseHeight > 0) {
                                    drawRoundRect(
                                        color = secondaryColor,
                                        topLeft = Offset(expenseX, expenseY),
                                        size = Size(barWidth, expenseHeight),
                                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No chart data available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
