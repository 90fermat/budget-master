package com.budgetmaster.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.inter
import budgetmaster.core.generated.resources.outfit
import org.jetbrains.compose.resources.Font

/**
 * BudgetMaster typography scale (DESIGN_SYSTEM.md §3).
 *
 * Display styles carry `fontFeatureSettings = "tnum"` (tabular figures) because they
 * render balances — this prevents layout wobble during count-up animations.
 *
 * Font families are currently platform sans-serif; bundling Outfit (headlines) and
 * Inter (body) via compose-resources is tracked in ROADMAP.md Phase 0.
 */
val AppTypography = Typography(
    // Balances & large amounts — always tabular figures
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        fontFeatureSettings = "tnum",
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontFeatureSettings = "tnum",
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        fontFeatureSettings = "tnum",
    ),
    // Core screen headers
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    // Section headers & card titles
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    // List content & body text
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    // Buttons, badges, tiny captions
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)

/**
 * Extra numeric styles that Material 3 [Typography] has no slot for.
 * Use for transaction amounts in lists (tabular figures mandatory).
 */
object FinancialTextStyles {
    /** Transaction amounts inside list rows. */
    val amountList = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = "tnum",
    )

    /** Compact amounts inside badges and pills. */
    val amountBadge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum",
    )
}

/** Outfit — geometric display/heading family (bundled variable font). */
@Composable
private fun outfitFamily() = FontFamily(
    Font(Res.font.outfit, FontWeight.Medium),
    Font(Res.font.outfit, FontWeight.SemiBold),
    Font(Res.font.outfit, FontWeight.Bold),
    Font(Res.font.outfit, FontWeight.ExtraBold),
)

/** Inter — highly readable body/label family (bundled variable font). */
@Composable
private fun interFamily() = FontFamily(
    Font(Res.font.inter, FontWeight.Normal),
    Font(Res.font.inter, FontWeight.Medium),
    Font(Res.font.inter, FontWeight.SemiBold),
    Font(Res.font.inter, FontWeight.Bold),
)

/**
 * The [AppTypography] scale with the bundled fonts applied: **Outfit** for
 * display/headline/title styles and **Inter** for body/label styles. Composable
 * because font resources load at runtime; call from within an `AppTheme`.
 */
@Composable
fun appTypography(): Typography {
    val outfit = outfitFamily()
    val inter = interFamily()
    return AppTypography.copy(
        displayLarge = AppTypography.displayLarge.copy(fontFamily = outfit),
        displayMedium = AppTypography.displayMedium.copy(fontFamily = outfit),
        displaySmall = AppTypography.displaySmall.copy(fontFamily = outfit),
        headlineLarge = AppTypography.headlineLarge.copy(fontFamily = outfit),
        headlineMedium = AppTypography.headlineMedium.copy(fontFamily = outfit),
        headlineSmall = AppTypography.headlineSmall.copy(fontFamily = outfit),
        titleLarge = AppTypography.titleLarge.copy(fontFamily = outfit),
        titleMedium = AppTypography.titleMedium.copy(fontFamily = outfit),
        titleSmall = AppTypography.titleSmall.copy(fontFamily = outfit),
        bodyLarge = AppTypography.bodyLarge.copy(fontFamily = inter),
        bodyMedium = AppTypography.bodyMedium.copy(fontFamily = inter),
        bodySmall = AppTypography.bodySmall.copy(fontFamily = inter),
        labelLarge = AppTypography.labelLarge.copy(fontFamily = inter),
        labelMedium = AppTypography.labelMedium.copy(fontFamily = inter),
        labelSmall = AppTypography.labelSmall.copy(fontFamily = inter),
    )
}
