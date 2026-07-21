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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.language_english
import budgetmaster.core.generated.resources.language_french
import budgetmaster.core.generated.resources.language_system
import budgetmaster.core.generated.resources.palette_amethyst
import budgetmaster.core.generated.resources.palette_dynamic
import budgetmaster.core.generated.resources.palette_emerald
import budgetmaster.core.generated.resources.palette_indigo
import budgetmaster.core.generated.resources.palette_sunset
import budgetmaster.core.generated.resources.settings_appearance
import budgetmaster.core.generated.resources.settings_currency
import budgetmaster.core.generated.resources.settings_language
import budgetmaster.core.generated.resources.settings_palette
import budgetmaster.core.generated.resources.settings_preferences
import budgetmaster.core.generated.resources.settings_replay_onboarding
import budgetmaster.core.generated.resources.settings_sign_out
import budgetmaster.core.generated.resources.settings_theme_mode
import budgetmaster.core.generated.resources.settings_title
import budgetmaster.core.generated.resources.theme_mode_dark
import budgetmaster.core.generated.resources.theme_mode_light
import budgetmaster.core.generated.resources.theme_mode_system
import budgetmaster.core.generated.resources.guide_tips_reset
import budgetmaster.core.generated.resources.guide_tips_reset_done
import budgetmaster.core.generated.resources.guide_tips_show
import budgetmaster.core.generated.resources.guide_tips_show_desc
import budgetmaster.core.generated.resources.guide_tips_title
import budgetmaster.core.generated.resources.settings_privacy_title
import budgetmaster.core.generated.resources.settings_secure_screen
import budgetmaster.core.generated.resources.settings_secure_screen_desc
import budgetmaster.core.generated.resources.settings_ai_title
import budgetmaster.core.generated.resources.settings_ai_enable
import budgetmaster.core.generated.resources.settings_ai_enable_desc
import budgetmaster.core.generated.resources.action_cancel
import budgetmaster.core.generated.resources.settings_delete_account
import budgetmaster.core.generated.resources.settings_delete_account_title
import budgetmaster.core.generated.resources.settings_delete_account_body
import budgetmaster.core.generated.resources.settings_delete_account_confirm
import budgetmaster.core.generated.resources.settings_delete_account_failed
import budgetmaster.core.generated.resources.settings_sms_title
import budgetmaster.core.generated.resources.settings_sms_no_wallets
import budgetmaster.core.generated.resources.settings_sms_destination_help
import budgetmaster.core.generated.resources.settings_sms_destination_label
import budgetmaster.core.generated.resources.settings_sms_enable
import budgetmaster.core.generated.resources.settings_sms_enable_desc
import budgetmaster.core.generated.resources.settings_sms_number_label
import budgetmaster.core.generated.resources.settings_sms_number_placeholder
import budgetmaster.core.generated.resources.settings_sms_number_help
import budgetmaster.core.generated.resources.settings_sms_permission_needed
import budgetmaster.core.generated.resources.settings_sms_backfill
import budgetmaster.core.generated.resources.settings_sms_backfill_running
import budgetmaster.core.generated.resources.settings_sms_backfill_done
import com.budgetmaster.core.designsystem.SurfaceLevel
import com.budgetmaster.core.designsystem.components.AppCard
import com.budgetmaster.core.designsystem.components.GuidanceHost
import com.budgetmaster.core.designsystem.components.GuidanceSheet
import com.budgetmaster.core.designsystem.components.rememberGuidance
import com.budgetmaster.core.guidance.GuidanceKey
import com.budgetmaster.core.guidance.GuidancePreferences
import com.budgetmaster.core.guidance.GuidanceRegistry
import com.budgetmaster.core.guidance.ScreenGuide
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.budgetmaster.core.designsystem.AppPalette
import com.budgetmaster.core.designsystem.DarkModeSetting
import com.budgetmaster.core.designsystem.DynamicSwatchColors
import com.budgetmaster.core.designsystem.Spacing
import com.budgetmaster.core.designsystem.colorScheme
import com.budgetmaster.core.localization.AppLanguage
import com.budgetmaster.core.notifications.rememberNotificationPermissionRequester
import com.budgetmaster.core.sms.rememberSmsPermissionRequester
import com.budgetmaster.core.sms.moneyProviderLabelRes
import com.budgetmaster.core.db.WalletRef
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import com.budgetmaster.core.currency.SUPPORTED_CURRENCY_CODES
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel


/**
 * Settings screen: appearance (theme mode, brand palette, language), preferences,
 * and account actions. All selections are persisted and applied app-wide instantly.
 *
 * @param onSignOut Callback invoked when the user requests to sign out.
 * @param onReplayOnboarding Callback invoked to navigate back into the onboarding intro.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onSignOut: () -> Unit = {},
    onReplayOnboarding: () -> Unit = {},
    /**
     * Runs account deletion. Returns success once the account and its data are gone (the shell
     * then navigates away); a failure — e.g. the provider needs a recent login — is shown inline
     * so the user knows to sign in again and retry.
     */
    onDeleteAccount: suspend () -> Result<Unit> = { Result.success(Unit) },
    /**
     * Imports mobile-money messages already on the device. Returns how many transactions were
     * created. Runs when the user switches import on, so the ledger starts with their history.
     */
    onBackfillMessages: suspend () -> Int = { 0 },
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val guidance = rememberGuidance(GuidanceKey.SETTINGS)
    GuidanceHost(guidance)

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

        SettingsCard {
            Text(
                text = stringResource(Res.string.settings_currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(Spacing.small))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small)) {
                SUPPORTED_CURRENCY_CODES.forEach { code ->
                    FilterChip(
                        selected = state.currency == code,
                        onClick = { viewModel.onIntent(SettingsIntent.CurrencySelected(code)) },
                        label = { Text(code) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.small))

        SettingRowAction(
            label = stringResource(Res.string.settings_replay_onboarding),
            onClick = {
                viewModel.onIntent(SettingsIntent.ReplayOnboarding)
                onReplayOnboarding()
            },
        )

        Spacer(modifier = Modifier.height(Spacing.medium))
        HelpAndTipsSection()

        Spacer(modifier = Modifier.height(Spacing.medium))
        SmsImportSection(
            enabled = state.smsImportEnabled,
            msisdns = state.smsOwnerMsisdns,
            providers = state.smsProviders,
            importAccounts = state.smsImportAccounts,
            wallets = state.wallets,
            onEnabledChange = { viewModel.onIntent(SettingsIntent.SmsImportEnabledChanged(it)) },
            onMsisdnsChange = { viewModel.onIntent(SettingsIntent.SmsOwnerMsisdnsChanged(it)) },
            onDestinationChange = { provider, accountId ->
                viewModel.onIntent(SettingsIntent.SmsImportAccountChanged(provider, accountId))
            },
            onBackfill = onBackfillMessages,
        )

        Spacer(modifier = Modifier.height(Spacing.medium))
        PrivacySection(
            secureScreen = state.secureScreen,
            onSecureScreenChange = { viewModel.onIntent(SettingsIntent.SecureScreenChanged(it)) },
        )

        Spacer(modifier = Modifier.height(Spacing.medium))
        AiSection(
            enabled = state.aiEnabled,
            onEnabledChange = { viewModel.onIntent(SettingsIntent.AiEnabledChanged(it)) },
        )

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
                .heightIn(min = 52.dp)
        ) {
            Text(
                text = stringResource(Res.string.settings_sign_out),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(Spacing.medium))
        DeleteAccountAction(onDeleteAccount = onDeleteAccount)
    }
}

/**
 * The "Delete account" destructive action, with a confirmation dialog spelling out that it's
 * permanent — this is the Play-required account-deletion path, and it wipes the ledger, not just
 * the session, so a mis-tap must not be enough to trigger it.
 */
@Composable
private fun DeleteAccountAction(onDeleteAccount: suspend () -> Result<Unit>) {
    val scope = rememberCoroutineScope()
    var showConfirm by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val failedMsg = stringResource(Res.string.settings_delete_account_failed)

    TextButton(
        onClick = { showConfirm = true },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_delete_account),
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold,
        )
    }
    error?.let {
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { if (!deleting) showConfirm = false },
            title = { Text(stringResource(Res.string.settings_delete_account_title)) },
            text = { Text(stringResource(Res.string.settings_delete_account_body)) },
            confirmButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = {
                        deleting = true
                        error = null
                        scope.launch {
                            onDeleteAccount()
                                .onSuccess { showConfirm = false } // shell navigates away
                                .onFailure { error = failedMsg; showConfirm = false }
                            deleting = false
                        }
                    },
                ) {
                    Text(
                        stringResource(Res.string.settings_delete_account_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(enabled = !deleting, onClick = { showConfirm = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
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
    AppPalette.AMETHYST -> Res.string.palette_amethyst
    AppPalette.SUNSET -> Res.string.palette_sunset
    AppPalette.DYNAMIC -> Res.string.palette_dynamic
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
            if (palette == AppPalette.DYNAMIC) {
                // Material You: show a multi-hue swatch since colors come from the wallpaper.
                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 14.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(DynamicSwatchColors))
                )
            } else {
                listOf(scheme.primary, scheme.secondary, scheme.tertiary).forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
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
    AppCard(level = SurfaceLevel.Raised) {
        Column {
            content()
        }
    }
}

/**
 * Browse every screen's guide on demand, and control whether they open by themselves.
 *
 * Enumerates [GuidanceRegistry] rather than a hand-written list, so a new screen's guide
 * appears here automatically instead of being forgotten.
 */
/**
 * Automatic mobile-money import.
 *
 * Hidden entirely where the platform has no SMS access (iOS, web), rather than showing a switch
 * that could never work. Turning it on requests the permission first — flipping the preference
 * without the grant would leave a setting that says "on" while nothing is captured.
 */
/**
 * A dropdown that chooses which wallet a provider's imported transactions land in.
 *
 * One per configured provider. The selection is persisted immediately; there is no "save" step,
 * consistent with every other setting on this screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportDestinationPicker(
    provider: String,
    wallets: List<WalletRef>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    val providerName = moneyProviderLabelRes(provider)?.let { stringResource(it) } ?: provider
    val selected = wallets.firstOrNull { it.id == selectedId }
    var expanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(vertical = Spacing.small)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = selected?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(Res.string.settings_sms_destination_label, providerName)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                wallets.forEach { wallet ->
                    DropdownMenuItem(
                        text = { Text(wallet.name) },
                        onClick = {
                            onSelect(wallet.id)
                            expanded = false
                        },
                    )
                }
            }
        }
        Text(
            text = stringResource(Res.string.settings_sms_destination_help, providerName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SmsImportSection(
    enabled: Boolean,
    msisdns: String,
    providers: List<String>,
    importAccounts: Map<String, String>,
    wallets: List<WalletRef>,
    onEnabledChange: (Boolean) -> Unit,
    onMsisdnsChange: (String) -> Unit,
    onDestinationChange: (provider: String, accountId: String?) -> Unit,
    onBackfill: suspend () -> Int,
) {
    val scope = rememberCoroutineScope()
    var backfilling by remember { mutableStateOf(false) }
    var backfilled by remember { mutableStateOf<Int?>(null) }
    var permissionDenied by remember { mutableStateOf(false) }

    // Fire-and-forget: a denied notification permission is not fatal, because every import is also
    // written to the in-app inbox. Requested here because enabling capture is when live captures
    // start needing to announce themselves.
    val notificationPermission = rememberNotificationPermissionRequester {}

    val permission = rememberSmsPermissionRequester { granted ->
        permissionDenied = !granted
        if (granted) {
            onEnabledChange(true)
            if (notificationPermission.isSupported && !notificationPermission.isGranted) {
                notificationPermission.request()
            }
            // Backfill immediately: a ledger that starts with the user's history is useful today,
            // one that starts empty is useful in a month.
            backfilling = true
            scope.launch {
                backfilled = runCatching { onBackfill() }.getOrDefault(0)
                backfilling = false
            }
        }
    }

    if (!permission.isSupported) return

    Text(
        text = stringResource(Res.string.settings_sms_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(Spacing.small))

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.settings_sms_enable),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(Res.string.settings_sms_enable_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(Spacing.small))
        Switch(
            checked = enabled,
            onCheckedChange = { wants ->
                permissionDenied = false
                if (!wants) {
                    onEnabledChange(false)
                } else if (permission.isGranted) {
                    onEnabledChange(true)
                } else {
                    permission.request()
                }
            },
        )
    }

    if (permissionDenied) {
        Text(
            text = stringResource(Res.string.settings_sms_permission_needed),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }

    if (enabled) {
        // Where each provider's captured transactions land. This is the fix for imports silently
        // going to the first wallet ever created: the destination is now the user's choice, shown
        // and changeable, defaulted to the active wallet when import was switched on.
        if (wallets.isEmpty()) {
            Text(
                text = stringResource(Res.string.settings_sms_no_wallets),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = Spacing.small),
            )
        } else {
            providers.forEach { provider ->
                ImportDestinationPicker(
                    provider = provider,
                    wallets = wallets,
                    selectedId = importAccounts[provider],
                    onSelect = { accountId -> onDestinationChange(provider, accountId) },
                )
            }
        }

        // The field owns its own text once the user starts typing.
        //
        // It used to render `msisdns` straight from the ViewModel, which meant every keystroke
        // made a round trip through a DataStore write and a 7-way combine before coming back --
        // and a plain String value carries no selection for Compose to preserve, so the cursor
        // snapped every time the delayed value landed. Holding a local draft and never reading
        // the echo back removes the round trip from the typing path entirely.
        //
        // Null means "nothing typed yet", so the persisted value still shows on first load. That
        // matters because the state flow starts at SettingsState() with an empty string and fills
        // in a moment later; seeding from it eagerly would have shown a blank field.
        var draft by rememberSaveable { mutableStateOf<String?>(null) }

        OutlinedTextField(
            value = draft ?: msisdns,
            onValueChange = { typed ->
                draft = typed
                onMsisdnsChange(typed)
            },
            label = { Text(stringResource(Res.string.settings_sms_number_label)) },
            placeholder = { Text(stringResource(Res.string.settings_sms_number_placeholder)) },
            supportingText = { Text(stringResource(Res.string.settings_sms_number_help)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )

        when {
            backfilling -> Text(
                text = stringResource(Res.string.settings_sms_backfill_running),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            backfilled != null -> Text(
                text = stringResource(Res.string.settings_sms_backfill_done, backfilled!!),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> TextButton(
                onClick = {
                    backfilling = true
                    scope.launch {
                        backfilled = runCatching { onBackfill() }.getOrDefault(0)
                        backfilling = false
                    }
                },
            ) { Text(stringResource(Res.string.settings_sms_backfill)) }
        }
    }
}

/**
 * The AI opt-in.
 *
 * The description spells out exactly what is and isn't sent, because "enable AI insights" alone
 * doesn't tell anyone that a summary of their money leaves the device. Off by default.
 */
/**
 * Screen-capture protection.
 *
 * Separate from the AI section because it protects something different: the AI switch governs what
 * leaves the device, this governs what a passer-by or the recents switcher can see.
 */
@Composable
private fun PrivacySection(
    secureScreen: Boolean,
    onSecureScreenChange: (Boolean) -> Unit,
) {
    Text(
        text = stringResource(Res.string.settings_privacy_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(Spacing.small))

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.settings_secure_screen),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(Res.string.settings_secure_screen_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(Spacing.small))
        Switch(checked = secureScreen, onCheckedChange = onSecureScreenChange)
    }
}

@Composable
private fun AiSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    Text(
        text = stringResource(Res.string.settings_ai_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(Spacing.small))

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.settings_ai_enable),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(Res.string.settings_ai_enable_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(Spacing.small))
        Switch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

@Composable
private fun HelpAndTipsSection() {
    val preferences = koinInject<GuidancePreferences>()
    val scope = rememberCoroutineScope()
    val tipsEnabled by preferences.tipsEnabled.collectAsState(initial = true)
    var openGuide by remember { mutableStateOf<ScreenGuide?>(null) }
    var resetDone by remember { mutableStateOf(false) }

    Text(
        text = stringResource(Res.string.guide_tips_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(Spacing.small))

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.guide_tips_show),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(Res.string.guide_tips_show_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = tipsEnabled,
            onCheckedChange = { enabled -> scope.launch { preferences.setTipsEnabled(enabled) } },
        )
    }

    // Every guide, on demand — the "?" only helps if you're already on the screen.
    GuidanceRegistry.guides.values.forEach { guide ->
        SettingRowAction(
            label = stringResource(guide.title),
            onClick = { openGuide = guide },
        )
    }

    SettingRowAction(
        label = stringResource(Res.string.guide_tips_reset),
        onClick = {
            scope.launch {
                preferences.resetAll()
                resetDone = true
            }
        },
    )
    if (resetDone) {
        Text(
            text = stringResource(Res.string.guide_tips_reset_done),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    openGuide?.let { guide ->
        GuidanceSheet(guide = guide, onDismiss = { openGuide = null })
    }
}

@Composable
private fun SettingRowLink(label: String, value: String) {
    AppCard(level = SurfaceLevel.Raised, contentPadding = 0.dp) {
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

@Composable
private fun SettingRowAction(label: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
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
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
