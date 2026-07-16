package com.budgetmaster.reports.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.budgetmaster.core.designsystem.financialColors
import com.budgetmaster.reports.domain.model.TrendPoint
import kotlin.math.max

/**
 * Paired income/expense bars per day.
 *
 * Compose `Canvas` for the same reason as the donut: identical on every target. Bars are
 * scaled to the largest single value so the shape stays readable regardless of magnitude.
 */
@Composable
fun TrendChart(
    points: List<TrendPoint>,
    description: String,
    modifier: Modifier = Modifier,
    height: Int = 160,
) {
    val incomeColor = MaterialTheme.financialColors.income
    val expenseColor = MaterialTheme.financialColors.expense
    val axis = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .semantics { contentDescription = description },
    ) {
        if (points.isEmpty()) return@Canvas

        val peak = points.maxOf { max(it.income, it.expenses) }
        if (peak <= 0.0) return@Canvas

        val baseline = size.height - 12f
        val slotWidth = size.width / points.size
        val barWidth = (slotWidth * 0.32f).coerceAtMost(14f)

        drawLine(
            color = axis,
            start = Offset(0f, baseline),
            end = Offset(size.width, baseline),
            strokeWidth = 1f,
        )

        points.forEachIndexed { index, point ->
            val centre = slotWidth * index + slotWidth / 2
            val incomeHeight = (point.income / peak).toFloat() * (baseline - 8f)
            val expenseHeight = (point.expenses / peak).toFloat() * (baseline - 8f)

            if (point.income > 0) {
                drawRect(
                    color = incomeColor,
                    topLeft = Offset(centre - barWidth - 1f, baseline - incomeHeight),
                    size = Size(barWidth, incomeHeight),
                )
            }
            if (point.expenses > 0) {
                drawRect(
                    color = expenseColor,
                    topLeft = Offset(centre + 1f, baseline - expenseHeight),
                    size = Size(barWidth, expenseHeight),
                )
            }
        }
    }
}
