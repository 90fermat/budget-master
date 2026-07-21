package com.budgetmaster.settings.domain.usecase

import com.budgetmaster.core.prefs.AppSettingsRepository
import com.budgetmaster.core.security.PinHasher

/**
 * The app-lock settings, grouped because they are only ever meaningful together.
 *
 * Hashing lives here rather than in the UI so a plain-text PIN exists for as short a time, and in
 * as few places, as possible: the screen hands over digits and immediately forgets them.
 */
class AppLockSettingsUseCase(private val repository: AppSettingsRepository) {

    suspend fun setEnabled(enabled: Boolean) = repository.setAppLockEnabled(enabled)

    suspend fun setPin(pin: String) = repository.setAppLockPinHash(PinHasher.hash(pin))

    suspend fun setBiometricEnabled(enabled: Boolean) = repository.setAppLockBiometricEnabled(enabled)

    suspend fun setTimeoutSeconds(seconds: Int) = repository.setAppLockTimeoutSeconds(seconds)
}
