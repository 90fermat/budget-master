package com.budgetmaster.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The four selectable brand palettes of BudgetMaster.
 *
 * Each palette provides a complete Material 3 [ColorScheme] for light and dark mode,
 * sharing the same premium slate/obsidian neutral foundation so switching palettes
 * only re-tints accent roles.
 *
 * @property id Stable identifier persisted in user preferences. Never rename.
 */
enum class AppPalette(val id: String) {
    /** Indigo "Trust" — the default BudgetMaster identity (DESIGN_SYSTEM.md palette). */
    INDIGO("indigo"),

    /** Emerald "Growth" — green-forward, savings-focused feel. */
    EMERALD("emerald"),

    /** Ocean "Calm" — sky/cyan blues. */
    OCEAN("ocean"),

    /** Sunset "Bold" — rose/amber warmth. */
    SUNSET("sunset");

    companion object {
        /** Palette applied when the user has never chosen one. */
        val Default = INDIGO

        /** Resolves a persisted [id] back to a palette, falling back to [Default]. */
        fun fromId(id: String?): AppPalette = entries.firstOrNull { it.id == id } ?: Default
    }
}

/** Returns the full Material 3 color scheme of this palette for the requested mode. */
fun AppPalette.colorScheme(darkTheme: Boolean): ColorScheme = when (this) {
    AppPalette.INDIGO -> if (darkTheme) IndigoDark else IndigoLight
    AppPalette.EMERALD -> if (darkTheme) EmeraldDark else EmeraldLight
    AppPalette.OCEAN -> if (darkTheme) OceanDark else OceanLight
    AppPalette.SUNSET -> if (darkTheme) SunsetDark else SunsetLight
}

// ── Shared neutral foundation (slate light / obsidian dark) ─────────────────

private fun premiumLightScheme(
    primary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    secondary: Color,
    secondaryContainer: Color,
    onSecondaryContainer: Color,
    tertiary: Color,
    tertiaryContainer: Color,
    onTertiaryContainer: Color,
): ColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = Color.White,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    onSecondary = Color.White,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = Color.White,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    inverseSurface = Color(0xFF1E293B),
    inverseOnSurface = Color(0xFFF8FAFC),
    inversePrimary = primaryContainer,
    surfaceTint = primary,
    scrim = Color.Black,
)

private fun premiumDarkScheme(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    secondary: Color,
    onSecondary: Color,
    secondaryContainer: Color,
    onSecondaryContainer: Color,
    tertiary: Color,
    onTertiary: Color,
    tertiaryContainer: Color,
    onTertiaryContainer: Color,
): ColorScheme = darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
    background = Color(0xFF0B0E14),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF131924),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1F293D),
    inverseSurface = Color(0xFFF8FAFC),
    inverseOnSurface = Color(0xFF0F172A),
    inversePrimary = primaryContainer,
    surfaceTint = primary,
    scrim = Color.Black,
)

// ── Indigo (default) ─────────────────────────────────────────────────────────

private val IndigoLight = premiumLightScheme(
    primary = Color(0xFF4F46E5),
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = Color(0xFF059669),
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Color(0xFF7C3AED),
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF4C1D95),
)

private val IndigoDark = premiumDarkScheme(
    primary = Color(0xFF6366F1),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF10B981),
    onSecondary = Color(0xFF022C22),
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFF8B5CF6),
    onTertiary = Color(0xFF2E1065),
    tertiaryContainer = Color(0xFF5B21B6),
    onTertiaryContainer = Color(0xFFEDE9FE),
)

// ── Emerald ──────────────────────────────────────────────────────────────────

private val EmeraldLight = premiumLightScheme(
    primary = Color(0xFF059669),
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFF0D9488),
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF134E4A),
    tertiary = Color(0xFF65A30D),
    tertiaryContainer = Color(0xFFECFCCB),
    onTertiaryContainer = Color(0xFF365314),
)

private val EmeraldDark = premiumDarkScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF022C22),
    primaryContainer = Color(0xFF065F46),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFFA3E635),
    onTertiary = Color(0xFF1A2E05),
    tertiaryContainer = Color(0xFF3F6212),
    onTertiaryContainer = Color(0xFFECFCCB),
)

// ── Ocean ────────────────────────────────────────────────────────────────────

private val OceanLight = premiumLightScheme(
    primary = Color(0xFF0284C7),
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary = Color(0xFF0891B2),
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF164E63),
    tertiary = Color(0xFF2563EB),
    tertiaryContainer = Color(0xFFDBEAFE),
    onTertiaryContainer = Color(0xFF1E3A8A),
)

private val OceanDark = premiumDarkScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF075985),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFF22D3EE),
    onSecondary = Color(0xFF083344),
    secondaryContainer = Color(0xFF155E75),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = Color(0xFF60A5FA),
    onTertiary = Color(0xFF172554),
    tertiaryContainer = Color(0xFF1E40AF),
    onTertiaryContainer = Color(0xFFDBEAFE),
)

// ── Sunset ───────────────────────────────────────────────────────────────────

private val SunsetLight = premiumLightScheme(
    primary = Color(0xFFE11D48),
    primaryContainer = Color(0xFFFFE4E6),
    onPrimaryContainer = Color(0xFF881337),
    secondary = Color(0xFFD97706),
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFFC026D3),
    tertiaryContainer = Color(0xFFFAE8FF),
    onTertiaryContainer = Color(0xFF701A75),
)

private val SunsetDark = premiumDarkScheme(
    primary = Color(0xFFFB7185),
    onPrimary = Color(0xFF4C0519),
    primaryContainer = Color(0xFF9F1239),
    onPrimaryContainer = Color(0xFFFFE4E6),
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF92400E),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = Color(0xFFE879F9),
    onTertiary = Color(0xFF4A044E),
    tertiaryContainer = Color(0xFF86198F),
    onTertiaryContainer = Color(0xFFFAE8FF),
)
