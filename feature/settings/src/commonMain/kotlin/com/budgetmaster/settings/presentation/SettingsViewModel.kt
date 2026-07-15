package com.budgetmaster.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetmaster.settings.domain.usecase.ObserveAppSettingsUseCase
import com.budgetmaster.settings.domain.usecase.ResetOnboardingUseCase
import com.budgetmaster.settings.domain.usecase.SetDarkModeUseCase
import com.budgetmaster.settings.domain.usecase.SetLanguageUseCase
import com.budgetmaster.settings.domain.usecase.SetPaletteUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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
    private val resetOnboarding: ResetOnboardingUseCase,
) : ViewModel() {

    /** Observable UI state, collected by the Settings Composable. */
    val state: StateFlow<SettingsState> = observeAppSettings()
        .map { SettingsState(palette = it.palette, darkMode = it.darkMode, language = it.language) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    /** Entry point for all UI events. */
    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.PaletteSelected -> viewModelScope.launch { setPalette(intent.palette) }
            is SettingsIntent.DarkModeSelected -> viewModelScope.launch { setDarkMode(intent.darkMode) }
            is SettingsIntent.LanguageSelected -> viewModelScope.launch { setLanguage(intent.language) }
            is SettingsIntent.ReplayOnboarding -> viewModelScope.launch { resetOnboarding() }
        }
    }
}
