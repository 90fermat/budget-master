package com.budgetmaster.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmaster.settings.domain.usecase.ObserveAppSettingsUseCase
import com.budgetmaster.settings.domain.usecase.ResetOnboardingUseCase
import com.budgetmaster.settings.domain.usecase.SetAiEnabledUseCase
import com.budgetmaster.settings.domain.usecase.SetSecureScreenUseCase
import com.budgetmaster.settings.domain.usecase.SetSmsImportEnabledUseCase
import com.budgetmaster.settings.domain.usecase.SetSmsOwnerMsisdnsUseCase
import com.budgetmaster.settings.domain.usecase.SetCurrencyUseCase
import com.budgetmaster.settings.domain.usecase.SetDarkModeUseCase
import com.budgetmaster.settings.domain.usecase.SetLanguageUseCase
import com.budgetmaster.settings.domain.usecase.SetPaletteUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import com.budgetmaster.settings.domain.usecase.SetSmsImportAccountUseCase
import com.budgetmaster.core.session.ActiveAccountStore
import com.budgetmaster.core.db.WalletDirectory
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * MVI ViewModel for the Settings screen: [SettingsIntent] → [SettingsState].
 *
 * State mirrors the persisted app settings so external changes (e.g. another window
 * on desktop/web) are reflected live.
 */
class SettingsViewModel(
    observeAppSettings: ObserveAppSettingsUseCase,
    private val setPalette: SetPaletteUseCase,
    private val setDarkMode: SetDarkModeUseCase,
    private val setLanguage: SetLanguageUseCase,
    private val setCurrency: SetCurrencyUseCase,
    private val setAiEnabled: SetAiEnabledUseCase,
    private val setSecureScreen: SetSecureScreenUseCase,
    private val setSmsImportEnabled: SetSmsImportEnabledUseCase,
    private val setSmsImportAccount: SetSmsImportAccountUseCase,
    private val setSmsOwnerMsisdns: SetSmsOwnerMsisdnsUseCase,
    private val resetOnboarding: ResetOnboardingUseCase,
    private val walletDirectory: WalletDirectory,
    activeAccountStore: ActiveAccountStore,
    /** Provider ids that have a parser, so only usable destinations are offered. */
    private val smsProviders: List<String>,
) : ViewModel() {

    /** Observable UI state, collected by the Settings Composable. */
    val state: StateFlow<SettingsState> = combine(
        observeAppSettings(),
        walletDirectory.observeWallets(),
        activeAccountStore.activeAccountId,
    ) { settings, wallets, activeAccountId ->
        SettingsState(
            palette = settings.palette,
            darkMode = settings.darkMode,
            language = settings.language,
            currency = settings.currency,
            aiEnabled = settings.aiEnabled,
            secureScreen = settings.secureScreen,
            smsImportEnabled = settings.smsImportEnabled,
            smsOwnerMsisdns = settings.smsOwnerMsisdns,
            smsProviders = smsProviders,
            smsImportAccounts = settings.smsImportAccounts,
            wallets = wallets,
            activeAccountId = activeAccountId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    /** Latest text typed into the mobile-money number field, debounced before it is persisted. */
    private val msisdnsInput = MutableSharedFlow<String>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        msisdnsInput
            .debounce(MSISDN_WRITE_DEBOUNCE_MS)
            .distinctUntilChanged()
            .onEach { setSmsOwnerMsisdns(it) }
            .launchIn(viewModelScope)
    }

    /** Entry point for all UI events. */
    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.PaletteSelected -> viewModelScope.launch { setPalette(intent.palette) }
            is SettingsIntent.DarkModeSelected -> viewModelScope.launch { setDarkMode(intent.darkMode) }
            is SettingsIntent.LanguageSelected -> viewModelScope.launch { setLanguage(intent.language) }
            is SettingsIntent.CurrencySelected -> viewModelScope.launch { setCurrency(intent.currencyCode) }
            is SettingsIntent.AiEnabledChanged -> viewModelScope.launch { setAiEnabled(intent.enabled) }
            is SettingsIntent.SecureScreenChanged -> viewModelScope.launch { setSecureScreen(intent.enabled) }
            is SettingsIntent.SmsImportEnabledChanged -> viewModelScope.launch {
                setSmsImportEnabled(intent.enabled)
                if (intent.enabled) defaultImportDestinations()
            }
            is SettingsIntent.SmsImportAccountChanged -> viewModelScope.launch {
                setSmsImportAccount(intent.provider, intent.accountId)
            }
            // Not a bare launch: one coroutine per keystroke gives no ordering guarantee, so
            // fast typing could land writes out of order and persist an older string. Funnelling
            // through a conflated flow keeps only the latest and writes it once the user pauses.
            is SettingsIntent.SmsOwnerMsisdnsChanged -> msisdnsInput.tryEmit(intent.msisdns)
            is SettingsIntent.ReplayOnboarding -> viewModelScope.launch { resetOnboarding() }
        }
    }

    /**
     * Gives every parser-backed provider a destination wallet when none is set.
     *
     * Run when import is switched on, so a captured message has somewhere to go without the user
     * having to choose first. Defaults to the active wallet - the one they were just looking at -
     * or the first wallet if none is active. Existing choices are left untouched, and if there are
     * no wallets yet nothing is set, which correctly leaves the NoDestination safety net in place.
     */
    private suspend fun defaultImportDestinations() {
        val current = state.value
        val fallback = current.activeAccountId ?: current.wallets.firstOrNull()?.id ?: return
        current.smsProviders
            .filter { it !in current.smsImportAccounts }
            .forEach { provider -> setSmsImportAccount(provider, fallback) }
    }

    private companion object {
        /** Long enough to cover typing a number, short enough that leaving the screen persists. */
        const val MSISDN_WRITE_DEBOUNCE_MS = 400L
    }
}
