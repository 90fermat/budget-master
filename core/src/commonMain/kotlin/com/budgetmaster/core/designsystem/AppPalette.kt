package com.budgetmaster.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * The selectable brand palettes of Budget Master.
 *
 * A curated premium set of four hand-tuned palettes plus [DYNAMIC] (Material You
 * extraction on Android 12+, falling back to [INDIGO] elsewhere). Each static palette
 * provides a complete Material 3 [ColorScheme] for light and dark mode on a shared,
 * jewel-toned neutral foundation.
 *
 * @property id Stable identifier persisted in user preferences. Never rename.
 */
enum class AppPalette(val id: String) {
    /** Midnight Indigo — the flagship Budget Master identity (default). */
    INDIGO("indigo"),

    /** Emerald — a deep jewel green, savings-forward. */
    EMERALD("emerald"),

    /** Amethyst — regal violet with fuchsia accents. */
    AMETHYST("amethyst"),

    /** Sunset Gold — warm rose and amber, luxe and expressive. */
    SUNSET("sunset"),

    /** Dynamic — Material You colors from the wallpaper (Android 12+), else Indigo. */
    DYNAMIC("dynamic");

    companion object {
        /** Palette applied when the user has never chosen one. */
        val Default = INDIGO

        /** Resolves a persisted [id] back to a palette, falling back to [Default]. */
        fun fromId(id: String?): AppPalette = entries.firstOrNull { it.id == id } ?: Default
    }
}

/**
 * Illustrative multi-hue swatch for the [AppPalette.DYNAMIC] option in Settings, since
 * its real colors are extracted from the device wallpaper at runtime.
 */
val DynamicSwatchColors: List<Color> = listOf(
    Color(0xFF6366F1), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEC4899),
)

/**
 * Returns the full Material 3 color scheme of this palette for the requested mode.
 * [DYNAMIC] returns the Indigo fallback here; the real Material You scheme is resolved
 * per-platform inside `AppTheme`.
 */
fun AppPalette.colorScheme(darkTheme: Boolean): ColorScheme = when (this) {
    AppPalette.INDIGO -> if (darkTheme) IndigoDark else IndigoLight
    AppPalette.EMERALD -> if (darkTheme) EmeraldDark else EmeraldLight
    AppPalette.AMETHYST -> if (darkTheme) AmethystDark else AmethystLight
    AppPalette.SUNSET -> if (darkTheme) SunsetDark else SunsetLight
    AppPalette.DYNAMIC -> if (darkTheme) IndigoDark else IndigoLight
}

// ── Shared premium neutral foundation ───────────────────────────────────────
// Light: soft slate paper. Dark: true obsidian with a faint blue cast and layered
// surface elevation for depth.

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
    inversePrimary = primaryContainer,
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
    background = Color(0xFFF6F8FC),
    onBackground = Color(0xFF0E1524),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E1524),
    surfaceVariant = Color(0xFFEEF2F9),
    onSurfaceVariant = Color(0xFF515C6E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9FBFE),
    surfaceContainer = Color(0xFFF1F5FA),
    surfaceContainerHigh = Color(0xFFEAEFF6),
    surfaceContainerHighest = Color(0xFFE3E9F2),
    outline = Color(0xFFC3CDDB),
    outlineVariant = Color(0xFFE1E7F0),
    inverseSurface = Color(0xFF1B2434),
    inverseOnSurface = Color(0xFFF3F6FB),
    surfaceTint = primary,
    scrim = Color(0xFF000000),
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
    inversePrimary = primaryContainer,
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
    background = Color(0xFF080B12),
    onBackground = Color(0xFFEDF1F7),
    surface = Color(0xFF10151F),
    onSurface = Color(0xFFEDF1F7),
    surfaceVariant = Color(0xFF1B2534),
    onSurfaceVariant = Color(0xFF9FABBD),
    surfaceContainerLowest = Color(0xFF06090F),
    surfaceContainerLow = Color(0xFF0E131C),
    surfaceContainer = Color(0xFF141A25),
    surfaceContainerHigh = Color(0xFF1B222F),
    surfaceContainerHighest = Color(0xFF232B3A),
    outline = Color(0xFF3A4557),
    outlineVariant = Color(0xFF232D3D),
    inverseSurface = Color(0xFFEDF1F7),
    inverseOnSurface = Color(0xFF0E1524),
    surfaceTint = primary,
    scrim = Color(0xFF000000),
)

// ── Midnight Indigo (default) ────────────────────────────────────────────────

private val IndigoLight = premiumLightScheme(
    primary = Color(0xFF4338CA),
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF262163),
    secondary = Color(0xFF0F766E),
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF0B3F3A),
    tertiary = Color(0xFF7C3AED),
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF4C1D95),
)

private val IndigoDark = premiumDarkScheme(
    primary = Color(0xFF8B93FF),
    onPrimary = Color(0xFF191553),
    primaryContainer = Color(0xFF322C8C),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF022C28),
    secondaryContainer = Color(0xFF0F5A52),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFFB69BFF),
    onTertiary = Color(0xFF2C1065),
    tertiaryContainer = Color(0xFF51309B),
    onTertiaryContainer = Color(0xFFEDE9FE),
)

// ── Emerald ──────────────────────────────────────────────────────────────────

private val EmeraldLight = premiumLightScheme(
    primary = Color(0xFF047857),
    primaryContainer = Color(0xFFCFFAE6),
    onPrimaryContainer = Color(0xFF033D2C),
    secondary = Color(0xFF0E7490),
    secondaryContainer = Color(0xFFCFF3FB),
    onSecondaryContainer = Color(0xFF083E4E),
    tertiary = Color(0xFF4D7C0F),
    tertiaryContainer = Color(0xFFE7F8C7),
    onTertiaryContainer = Color(0xFF2C4A06),
)

private val EmeraldDark = premiumDarkScheme(
    primary = Color(0xFF34E3A8),
    onPrimary = Color(0xFF00341F),
    primaryContainer = Color(0xFF065F43),
    onPrimaryContainer = Color(0xFFCFFAE6),
    secondary = Color(0xFF38D0EF),
    onSecondary = Color(0xFF04333F),
    secondaryContainer = Color(0xFF0C5063),
    onSecondaryContainer = Color(0xFFCFF3FB),
    tertiary = Color(0xFFAEE454),
    onTertiary = Color(0xFF223500),
    tertiaryContainer = Color(0xFF3C5E0C),
    onTertiaryContainer = Color(0xFFE7F8C7),
)

// ── Amethyst ─────────────────────────────────────────────────────────────────

private val AmethystLight = premiumLightScheme(
    primary = Color(0xFF7E22CE),
    primaryContainer = Color(0xFFF3E4FF),
    onPrimaryContainer = Color(0xFF44136F),
    secondary = Color(0xFFC026D3),
    secondaryContainer = Color(0xFFFCE0FF),
    onSecondaryContainer = Color(0xFF63106B),
    tertiary = Color(0xFF4F46E5),
    tertiaryContainer = Color(0xFFE0E7FF),
    onTertiaryContainer = Color(0xFF262163),
)

private val AmethystDark = premiumDarkScheme(
    primary = Color(0xFFC79BFF),
    onPrimary = Color(0xFF360764),
    primaryContainer = Color(0xFF5B1B95),
    onPrimaryContainer = Color(0xFFF3E4FF),
    secondary = Color(0xFFF29BFF),
    onSecondary = Color(0xFF530A5B),
    secondaryContainer = Color(0xFF8A1C93),
    onSecondaryContainer = Color(0xFFFCE0FF),
    tertiary = Color(0xFF9DA2FF),
    onTertiary = Color(0xFF1D1B6E),
    tertiaryContainer = Color(0xFF3730A3),
    onTertiaryContainer = Color(0xFFE0E7FF),
)

// ── Sunset Gold ──────────────────────────────────────────────────────────────

private val SunsetLight = premiumLightScheme(
    primary = Color(0xFFE11D48),
    primaryContainer = Color(0xFFFFE1E7),
    onPrimaryContainer = Color(0xFF7C0A28),
    secondary = Color(0xFFEA580C),
    secondaryContainer = Color(0xFFFFE6D2),
    onSecondaryContainer = Color(0xFF7A2C06),
    tertiary = Color(0xFFCA8A04),
    tertiaryContainer = Color(0xFFFCF0C8),
    onTertiaryContainer = Color(0xFF6B4A03),
)

private val SunsetDark = premiumDarkScheme(
    primary = Color(0xFFFF8098),
    onPrimary = Color(0xFF5C0620),
    primaryContainer = Color(0xFFA8123A),
    onPrimaryContainer = Color(0xFFFFE1E7),
    secondary = Color(0xFFFF9A5B),
    onSecondary = Color(0xFF4E1B03),
    secondaryContainer = Color(0xFF9C3908),
    onSecondaryContainer = Color(0xFFFFE6D2),
    tertiary = Color(0xFFF5CB4B),
    onTertiary = Color(0xFF3E2E00),
    tertiaryContainer = Color(0xFF8A6103),
    onTertiaryContainer = Color(0xFFFCF0C8),
)
