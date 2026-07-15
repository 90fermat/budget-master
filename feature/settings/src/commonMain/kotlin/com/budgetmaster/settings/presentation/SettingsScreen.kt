package com.budgetmaster.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.language_english
import budgetmaster.core.generated.resources.language_french
import budgetmaster.core.generated.resources.language_system
import budgetmaster.core.generated.resources.palette_emerald
import budgetmaster.core.generated.resources.palette_indigo
import budgetmaster.core.generated.resources.palette_ocean
import budgetmaster.core.generated.resources.palette_sunset
import budgetmaster.core.generated.resources.settings_appearance
import budgetmaster.core.generated.resources.settings_currency
import budgetmaster.core.generated.resources.settings_language
import budgetmaster.core.generated.resources.settings_palette
import budgetmaster.core.generated.resources.settings_preferences
import budgetmaster.core.generated.resources.settings_sign_out
import budgetmaster.core.generated.resources.settings_theme_mode
import budgetmaster.core.generated.resources.settings_title
import budgetmaster.core.generated.resources.theme_mode_dark
import budgetmaster.core.generated.resources.theme_mode_light
import budgetmaster.core.generated.resources.theme_mode_system
import com.budgetmaster.core.designsystem.AppPalette
import com.budgetmaster.core.designsystem.DarkModeSetting
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.designsystem.colorScheme
import com.budgetmaster.core.localization.AppLanguage
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Settings screen: appearance (theme mode, brand palette, language), preferences,
 * and account actions. All selections are persisted and applied app-wide instantly.
 *
 * @param onSignOut Callback invoked when the user requests to sign out.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onSignOut: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(Spacing.medium)
    ) {
        Text(
            text = stringResource(Res.string.settings_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(Spacing.large))

        // ── Appearance ───────────────────────────────────────────────────────
        SectionHeader(text = stringResource(Res.string.settings_appearance))

        SettingsCard {
            Text(
                text = stringResource(Res.string.settings_theme_mode),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.small))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                DarkModeSetting.entries.forEach { mode ->
                    FilterChip(
                        selected = state.darkMode == mode,
                        onClick = { viewModel.onIntent(SettingsIntent.DarkModeSelected(mode)) },
                        label = { Text(stringResource(mode.labelRes())) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            Text(
                text = stringResource(Res.string.settings_palette),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.small))
            val isDark = when (state.darkMode) {
                DarkModeSetting.SYSTEM -> isSystemInDarkTheme()
                DarkModeSetting.LIGHT -> false
                DarkModeSetting.DARK -> true
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                AppPalette.entries.forEach { palette ->
                    PaletteSwatch(
                        palette = palette,
                        isDark = isDark,
                        selected = state.palette == palette,
                        onClick = { viewModel.onIntent(SettingsIntent.PaletteSelected(palette)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.medium))

            Text(
                text = stringResource(Res.string.settings_language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.small))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                AppLanguage.entries.forEach { language ->
                    FilterChip(
                        selected = state.language == language,
                        onClick = { viewModel.onIntent(SettingsIntent.LanguageSelected(language)) },
                        label = { Text(stringResource(language.labelRes())) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.large))

        // ── Preferences ──────────────────────────────────────────────────────
        SectionHeader(text = stringResource(Res.string.settings_preferences))

        SettingRowLink(label = stringResource(Res.string.settings_currency), value = "USD")

        Spacer(modifier = Modifier.height(Spacing.huge))

        // ── Account ──────────────────────────────────────────────────────────
        Button(
            onClick = onSignOut,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = stringResource(Res.string.settings_sign_out),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

private fun DarkModeSetting.labelRes(): StringResource = when (this) {
    DarkModeSetting.SYSTEM -> Res.string.theme_mode_system
    DarkModeSetting.LIGHT -> Res.string.theme_mode_light
    DarkModeSetting.DARK -> Res.string.theme_mode_dark
}

private fun AppLanguage.labelRes(): StringResource = when (this) {
    AppLanguage.SYSTEM -> Res.string.language_system
    AppLanguage.ENGLISH -> Res.string.language_english
    AppLanguage.FRENCH -> Res.string.language_french
}

private fun AppPalette.labelRes(): StringResource = when (this) {
    AppPalette.INDIGO -> Res.string.palette_indigo
    AppPalette.EMERALD -> Res.string.palette_emerald
    AppPalette.OCEAN -> Res.string.palette_ocean
    AppPalette.SUNSET -> Res.string.palette_sunset
}

/**
 * A selectable palette preview: three accent dots (primary, secondary, tertiary)
 * above the palette name, outlined when selected.
 */
@Composable
private fun PaletteSwatch(
    palette: AppPalette,
    isDark: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = palette.colorScheme(isDark)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (selected) 2.dp else 1.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.compact, horizontal = Spacing.micro)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.micro)) {
            listOf(scheme.primary, scheme.secondary, scheme.tertiary).forEach { color ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.small))
        Text(
            text = stringResource(palette.labelRes()),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(Spacing.compact))
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(Spacing.medium)) {
            content()
        }
    }
}

@Composable
private fun SettingRowLink(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
