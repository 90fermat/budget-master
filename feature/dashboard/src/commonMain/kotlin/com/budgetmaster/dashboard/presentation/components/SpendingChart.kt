package com.budgetmaster.dashboard.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.budgetmaster.dashboard.domain.model.ChartPoint

/**
 * Representation of chart transition animations.
 * [Differential] animates point updates by interpolating differences in Y positions.
 */
enum class ChartAnimation {
    Differential
}

/**
 * Premium personal finance interactive cash flow chart.
 * Allows toggling between a Line chart (balance) and a Column chart (income/expenses).
 *
 * Android renders this with Vico, which brings axes and a touch marker. Vico ships Android
 * artifacts only, so iOS and web share a Compose Canvas rendering instead.
 *
 * @param chartData The cash-flow data points to plot.
 * @param modifier The modifier to be applied to the layout.
 * @param chartAnimation The entry and update transition animation mode.
 */
@Composable
expect fun SpendingChart(
    chartData: List<ChartPoint>,
    modifier: Modifier = Modifier,
    chartAnimation: ChartAnimation = ChartAnimation.Differential
)
