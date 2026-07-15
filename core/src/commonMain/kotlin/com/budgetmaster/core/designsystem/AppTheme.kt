package com.budgetmaster.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Root theme of BudgetMaster. Wraps [MaterialTheme] with the selected [AppPalette]
 * color scheme, the shared [AppTypography] scale, and the palette-independent
 * [FinancialColors] (via [LocalFinancialColors]).
 *
 * Every screen must render inside this theme; no `Color(0x…)` literals are allowed
 * outside this package (ARCHITECTURE.md rule).
 *
 * @param palette The user-selected brand palette (persisted in preferences).
 * @param darkTheme Whether to render the dark variant; defaults to the system setting.
 * @param content The app UI.
 */
@Composable
fun AppTheme(
    palette: AppPalette = AppPalette.Default,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val financialColors = if (darkTheme) DarkFinancialColors else LightFinancialColors
    CompositionLocalProvider(LocalFinancialColors provides financialColors) {
        MaterialTheme(
            colorScheme = palette.colorScheme(darkTheme),
            typography = AppTypography,
            content = content,
        )
    }
}
