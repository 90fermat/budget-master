package com.budgetmaster.dashboard.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.budgetmaster.dashboard.domain.model.ChartPoint

/**
 * Android draws the chart with the same Compose Canvas implementation as every other target.
 *
 * It previously used Vico, which meant one chart with two visual languages — different type,
 * spacing and colour handling depending on the platform, and a palette the app's five themes had
 * no say over. A single implementation reads as deliberate, honours the theme everywhere, and
 * drops a dependency.
 */
@Composable
actual fun SpendingChart(
    chartData: List<ChartPoint>,
    modifier: Modifier,
    chartAnimation: ChartAnimation,
) {
    CanvasSpendingChart(chartData = chartData, modifier = modifier)
}
