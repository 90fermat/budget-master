package com.budgetmaster.settings.domain.usecase

import com.budgetmaster.core.designsystem.AppPalette
import com.budgetmaster.core.designsystem.DarkModeSetting
import com.budgetmaster.core.localization.AppLanguage
import com.budgetmaster.core.prefs.AppSettings
import com.budgetmaster.core.prefs.AppSettingsRepository
import com.budgetmaster.core.prefs.OnboardingPreferences
import kotlinx.coroutines.flow.Flow

/** Streams the persisted [AppSettings] (palette, dark mode, language). */
class ObserveAppSettingsUseCase(private val repository: AppSettingsRepository) {
    operator fun invoke(): Flow<AppSettings> = repository.settings
}

/** Persists the selected brand [AppPalette]. */
class SetPaletteUseCase(private val repository: AppSettingsRepository) {
    suspend operator fun invoke(palette: AppPalette) = repository.setPalette(palette)
}

/** Persists the selected [DarkModeSetting]. */
class SetDarkModeUseCase(private val repository: AppSettingsRepository) {
    suspend operator fun invoke(darkMode: DarkModeSetting) = repository.setDarkMode(darkMode)
}

/** Persists the selected [AppLanguage]. */
class SetLanguageUseCase(private val repository: AppSettingsRepository) {
    suspend operator fun invoke(language: AppLanguage) = repository.setLanguage(language)
}

/** Persists the selected primary currency (ISO-4217 code). */
class SetCurrencyUseCase(private val repository: AppSettingsRepository) {
    suspend operator fun invoke(currencyCode: String) = repository.setCurrency(currencyCode)
}

/** Clears the onboarding-completed flag so the intro is shown again. */
class ResetOnboardingUseCase(private val onboardingPreferences: OnboardingPreferences) {
    suspend operator fun invoke() = onboardingPreferences.setCompleted(false)
}
