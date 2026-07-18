// Uses the experimental test dispatcher APIs, which is the point of these tests.
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.budgetmaster.settings.presentation

import com.budgetmaster.core.designsystem.AppPalette
import com.budgetmaster.core.designsystem.DarkModeSetting
import com.budgetmaster.core.localization.AppLanguage
import com.budgetmaster.core.prefs.AppSettingsRepository
import com.budgetmaster.core.prefs.KeyValueStore
import com.budgetmaster.core.prefs.OnboardingPreferences
import com.budgetmaster.settings.domain.usecase.ObserveAppSettingsUseCase
import com.budgetmaster.settings.domain.usecase.ResetOnboardingUseCase
import com.budgetmaster.settings.domain.usecase.SetAiEnabledUseCase
import com.budgetmaster.settings.domain.usecase.SetSmsImportEnabledUseCase
import com.budgetmaster.settings.domain.usecase.SetSmsOwnerMsisdnsUseCase
import com.budgetmaster.settings.domain.usecase.SetCurrencyUseCase
import com.budgetmaster.settings.domain.usecase.SetDarkModeUseCase
import com.budgetmaster.settings.domain.usecase.SetLanguageUseCase
import com.budgetmaster.settings.domain.usecase.SetPaletteUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** In-memory [KeyValueStore] so the ViewModel drives a real [AppSettingsRepository]. */
private class FakeStore : KeyValueStore {
    private val entries = MutableStateFlow<Map<String, String>>(emptyMap())
    override fun observeString(key: String): Flow<String?> = entries.map { it[key] }
    override suspend fun putString(key: String, value: String) {
        entries.value = entries.value + (key to value)
    }
    override suspend fun remove(key: String) { entries.value = entries.value - key }
}

class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: AppSettingsRepository

    private fun viewModel(): SettingsViewModel {
        repository = AppSettingsRepository(FakeStore())
        return SettingsViewModel(
            observeAppSettings = ObserveAppSettingsUseCase(repository),
            setPalette = SetPaletteUseCase(repository),
            setDarkMode = SetDarkModeUseCase(repository),
            setLanguage = SetLanguageUseCase(repository),
            setCurrency = SetCurrencyUseCase(repository),
            setAiEnabled = SetAiEnabledUseCase(repository),
            setSmsImportEnabled = SetSmsImportEnabledUseCase(repository),
            setSmsOwnerMsisdns = SetSmsOwnerMsisdnsUseCase(repository),
            resetOnboarding = ResetOnboardingUseCase(OnboardingPreferences(FakeStore())),
        )
    }

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    // state is stateIn(WhileSubscribed), so it only collects upstream while something subscribes;
    // without this the value stays at the initial default and the assertions are meaningless.
    private fun TestScope.keepStateHot(vm: SettingsViewModel) {
        backgroundScope.launch(dispatcher) { vm.state.collect {} }
    }

    @Test
    fun `starts from defaults`() = runTest(dispatcher) {
        val vm = viewModel()
        keepStateHot(vm)
        advanceUntilIdle()
        val state = vm.state.value
        assertEquals(AppPalette.Default, state.palette)
        assertEquals(AppLanguage.Default, state.language)
        // AI is opt-in: it must start off, or an absent preference would read as consent.
        assertFalse(state.aiEnabled)
    }

    @Test
    fun `each selection persists and is reflected in state`() = runTest(dispatcher) {
        val vm = viewModel()
        keepStateHot(vm)
        advanceUntilIdle()

        vm.onIntent(SettingsIntent.PaletteSelected(AppPalette.EMERALD))
        vm.onIntent(SettingsIntent.DarkModeSelected(DarkModeSetting.DARK))
        vm.onIntent(SettingsIntent.LanguageSelected(AppLanguage.FRENCH))
        vm.onIntent(SettingsIntent.CurrencySelected("EUR"))
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(AppPalette.EMERALD, state.palette)
        assertEquals(DarkModeSetting.DARK, state.darkMode)
        assertEquals(AppLanguage.FRENCH, state.language)
        assertEquals("EUR", state.currency)
    }

    @Test
    fun `sms import settings round-trip`() = runTest(dispatcher) {
        val vm = viewModel()
        keepStateHot(vm)
        advanceUntilIdle()

        // Reading someone's messages is the most invasive thing this app does, so it must start off.
        assertFalse(vm.state.value.smsImportEnabled)

        vm.onIntent(SettingsIntent.SmsImportEnabledChanged(true))
        vm.onIntent(SettingsIntent.SmsOwnerMsisdnsChanged("659228030"))
        advanceUntilIdle()

        assertTrue(vm.state.value.smsImportEnabled)
        assertEquals("659228030", vm.state.value.smsOwnerMsisdns)
    }

    @Test
    fun `toggling AI on then off flows through to state`() = runTest(dispatcher) {
        val vm = viewModel()
        keepStateHot(vm)
        advanceUntilIdle()

        vm.onIntent(SettingsIntent.AiEnabledChanged(true))
        advanceUntilIdle()
        assertTrue(vm.state.value.aiEnabled)

        vm.onIntent(SettingsIntent.AiEnabledChanged(false))
        advanceUntilIdle()
        assertFalse(vm.state.value.aiEnabled)
    }
}
