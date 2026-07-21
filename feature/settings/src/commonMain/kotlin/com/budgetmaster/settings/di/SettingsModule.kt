package com.budgetmaster.settings.di

import com.budgetmaster.settings.domain.usecase.ObserveAppSettingsUseCase
import com.budgetmaster.settings.domain.usecase.ResetOnboardingUseCase
import com.budgetmaster.settings.domain.usecase.SetAiEnabledUseCase
import com.budgetmaster.settings.domain.usecase.SetSecureScreenUseCase
import com.budgetmaster.settings.domain.usecase.SetSmsImportAccountUseCase
import com.budgetmaster.settings.domain.usecase.SetSmsImportEnabledUseCase
import com.budgetmaster.settings.domain.usecase.SetSmsOwnerMsisdnsUseCase
import com.budgetmaster.settings.domain.usecase.SetCurrencyUseCase
import com.budgetmaster.settings.domain.usecase.SetDarkModeUseCase
import com.budgetmaster.settings.domain.usecase.SetLanguageUseCase
import com.budgetmaster.settings.domain.usecase.SetPaletteUseCase
import com.budgetmaster.core.sms.MoneyMessageParser
import com.budgetmaster.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for the Settings feature dependencies.
 */
val settingsModule = module {
    // Use cases
    factory { ObserveAppSettingsUseCase(get()) }
    factory { SetPaletteUseCase(get()) }
    factory { SetDarkModeUseCase(get()) }
    factory { SetLanguageUseCase(get()) }
    factory { SetCurrencyUseCase(get()) }
    factory { SetAiEnabledUseCase(get()) }
    factory { SetSecureScreenUseCase(get()) }
    factory { SetSmsImportEnabledUseCase(get()) }
    factory { SetSmsImportAccountUseCase(get()) }
    factory { SetSmsOwnerMsisdnsUseCase(get()) }
    factory { ResetOnboardingUseCase(get()) }

    // ViewModels
    viewModel {
        SettingsViewModel(
            observeAppSettings = get(),
            setPalette = get(),
            setDarkMode = get(),
            setLanguage = get(),
            setCurrency = get(),
            setAiEnabled = get(),
            setSecureScreen = get(),
            setSmsImportEnabled = get(),
            setSmsImportAccount = get(),
            setSmsOwnerMsisdns = get(),
            resetOnboarding = get(),
            walletDirectory = get(),
            activeAccountStore = get(),
            // Only providers with a parser, so Settings never offers a destination for messages
            // that can never be read. Self-updating: the MTN parser appearing adds its row.
            smsProviders = get<List<MoneyMessageParser>>().map { it.provider },
        )
    }
}
