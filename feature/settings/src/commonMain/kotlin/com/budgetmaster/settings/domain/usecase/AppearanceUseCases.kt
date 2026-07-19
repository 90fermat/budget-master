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

/**
 * Records whether the user opts in to AI features.
 *
 * Off means no spending summary is sent to a cloud model at all.
 */
class SetAiEnabledUseCase(private val repository: AppSettingsRepository) {
    suspend operator fun invoke(enabled: Boolean) = repository.setAiEnabled(enabled)
}

/** Turns automatic mobile-money message import on or off. */
class SetSecureScreenUseCase(private val repository: AppSettingsRepository) {
    suspend operator fun invoke(enabled: Boolean) = repository.setSecureScreen(enabled)
}

class SetSmsImportEnabledUseCase(private val repository: AppSettingsRepository) {
    suspend operator fun invoke(enabled: Boolean) = repository.setSmsImportEnabled(enabled)
}

/** Records the user's own mobile-money number(s), which resolve transfer direction. */
class SetSmsOwnerMsisdnsUseCase(private val repository: AppSettingsRepository) {
    suspend operator fun invoke(msisdns: String) = repository.setSmsOwnerMsisdns(msisdns)
}

/** Clears the onboarding-completed flag so the intro is shown again. */
class ResetOnboardingUseCase(private val onboardingPreferences: OnboardingPreferences) {
    suspend operator fun invoke() = onboardingPreferences.setCompleted(false)
}
