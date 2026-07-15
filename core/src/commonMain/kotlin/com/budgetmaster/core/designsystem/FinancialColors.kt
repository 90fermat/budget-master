package com.budgetmaster.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors for financial data. These are intentionally **stable across all
 * [AppPalette] choices** — income is always green and expenses always red, regardless
 * of the selected brand palette, so users never re-learn the meaning of a color.
 *
 * Access via `MaterialTheme.financialColors` inside an `AppTheme`.
 */
@Immutable
data class FinancialColors(
    /** Cash inflow, positive balances, growth indicators. */
    val income: Color,
    /** Cash outflow, negative trends. */
    val expense: Color,
    /** Approaching-limit warnings (e.g. budget ≥ 85%). */
    val warning: Color,
    /** Category chart color — Food & Dining. */
    val chartFood: Color,
    /** Category chart color — Housing & Bills. */
    val chartHousing: Color,
    /** Category chart color — Shopping. */
    val chartShopping: Color,
    /** Category chart color — Travel. */
    val chartTravel: Color,
    /** Category chart color — Entertainment. */
    val chartEntertainment: Color,
)

/** Light-mode financial colors (DESIGN_SYSTEM.md §2). */
val LightFinancialColors = FinancialColors(
    income = Color(0xFF059669),
    expense = Color(0xFFDC2626),
    warning = Color(0xFFF59E0B),
    chartFood = Color(0xFFF59E0B),
    chartHousing = Color(0xFF3B82F6),
    chartShopping = Color(0xFFEC4899),
    chartTravel = Color(0xFF14B8A6),
    chartEntertainment = Color(0xFF8B5CF6),
)

/** Dark-mode financial colors (brighter variants for dark surfaces). */
val DarkFinancialColors = FinancialColors(
    income = Color(0xFF10B981),
    expense = Color(0xFFF87171),
    warning = Color(0xFFFBBF24),
    chartFood = Color(0xFFFBBF24),
    chartHousing = Color(0xFF60A5FA),
    chartShopping = Color(0xFFF472B6),
    chartTravel = Color(0xFF2DD4BF),
    chartEntertainment = Color(0xFFA78BFA),
)

/** CompositionLocal carrying the active [FinancialColors]; provided by [AppTheme]. */
val LocalFinancialColors = staticCompositionLocalOf { LightFinancialColors }

/** Theme-aware accessor: `MaterialTheme.financialColors.income`. */
val MaterialTheme.financialColors: FinancialColors
    @Composable
    @ReadOnlyComposable
    get() = LocalFinancialColors.current
