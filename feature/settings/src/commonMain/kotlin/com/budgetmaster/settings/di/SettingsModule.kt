package com.budgetmaster.settings.di

import com.budgetmaster.settings.domain.usecase.ObserveAppSettingsUseCase
import com.budgetmaster.settings.domain.usecase.SetDarkModeUseCase
import com.budgetmaster.settings.domain.usecase.SetLanguageUseCase
import com.budgetmaster.settings.domain.usecase.SetPaletteUseCase
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

    // ViewModels
    viewModel { SettingsViewModel(get(), get(), get(), get()) }
}
