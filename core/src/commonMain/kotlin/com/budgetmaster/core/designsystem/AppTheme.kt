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
 * The [AppPalette.DYNAMIC] palette resolves to a Material You scheme on Android 12+ and
 * falls back to the default palette on other platforms/older Android.
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
    val dynamicScheme = if (palette == AppPalette.DYNAMIC) dynamicColorSchemeOrNull(darkTheme) else null
    val colorScheme = dynamicScheme ?: palette.colorScheme(darkTheme)
    val financialColors = if (darkTheme) DarkFinancialColors else LightFinancialColors
    CompositionLocalProvider(LocalFinancialColors provides financialColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
