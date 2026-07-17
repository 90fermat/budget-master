package com.budgetmaster.dashboard.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.budgetmaster.dashboard.domain.model.ChartPoint

/**
 * iOS draws the chart with Compose Canvas: Vico is an Android-only library, so it cannot back
 * this target. This file previously held a copy of the Android/Vico implementation, which meant
 * the iOS target never compiled.
 */
@Composable
actual fun SpendingChart(
    chartData: List<ChartPoint>,
    modifier: Modifier,
    chartAnimation: ChartAnimation,
) {
    CanvasSpendingChart(chartData = chartData, modifier = modifier)
}
