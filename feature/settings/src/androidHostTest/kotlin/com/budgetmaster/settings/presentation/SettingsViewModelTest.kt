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
import com.budgetmaster.settings.domain.usecase.SetSecureScreenUseCase
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
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.budgetmaster.core.db.BudgetMasterDatabase
import com.budgetmaster.core.db.DatabaseProvider
import com.budgetmaster.core.db.DefaultData
import com.budgetmaster.core.db.WalletDirectory
import com.budgetmaster.core.session.ActiveAccountStore
import com.budgetmaster.core.session.SessionStore
import com.budgetmaster.core.sms.MoneyProviders
import com.budgetmaster.settings.domain.usecase.AppLockSettingsUseCase
import com.budgetmaster.settings.domain.usecase.SetSmsImportAccountUseCase
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
    private lateinit var database: BudgetMasterDatabase
    private lateinit var activeAccountStore: ActiveAccountStore

    private fun viewModel(
        smsProviders: List<String> = emptyList(),
    ): SettingsViewModel {
        repository = AppSettingsRepository(FakeStore())
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BudgetMasterDatabase.Schema.synchronous().create(driver)
        database = BudgetMasterDatabase(driver)
        val sessionStore = SessionStore()
        activeAccountStore = ActiveAccountStore(FakeStore())
        return SettingsViewModel(
            observeAppSettings = ObserveAppSettingsUseCase(repository),
            setPalette = SetPaletteUseCase(repository),
            setDarkMode = SetDarkModeUseCase(repository),
            setLanguage = SetLanguageUseCase(repository),
            setCurrency = SetCurrencyUseCase(repository),
            setAiEnabled = SetAiEnabledUseCase(repository),
            setSecureScreen = SetSecureScreenUseCase(repository),
            setSmsImportEnabled = SetSmsImportEnabledUseCase(repository),
            setSmsImportAccount = SetSmsImportAccountUseCase(repository),
            setAppLock = AppLockSettingsUseCase(repository),
            setSmsOwnerMsisdns = SetSmsOwnerMsisdnsUseCase(repository),
            resetOnboarding = ResetOnboardingUseCase(OnboardingPreferences(FakeStore())),
            walletDirectory = WalletDirectory(DatabaseProvider(database), sessionStore, dispatcher),
            activeAccountStore = activeAccountStore,
            smsProviders = smsProviders,
        )
    }

    /** Seeds a default user + one wallet so the wallet directory has something to return. */
    private suspend fun seedWallet(id: String, name: String) {
        val q = database.budgetMasterDatabaseQueries
        q.insertUser(DefaultData.DEFAULT_USER_ID, "You", "you@x.com", "USD", 0L)
        q.insertAccount(id, DefaultData.DEFAULT_USER_ID, name, "CASH", 0.0, "USD", 0L, 0)
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

    @Test
    fun `enabling import defaults each provider destination to the active wallet`() = runTest(dispatcher) {
        val vm = viewModel(smsProviders = listOf(MoneyProviders.ORANGE_MONEY))
        keepStateHot(vm)
        seedWallet("w_active", "Everyday")
        seedWallet("w_other", "Savings")
        activeAccountStore.setActiveAccount("w_active")
        advanceUntilIdle()

        vm.onIntent(SettingsIntent.SmsImportEnabledChanged(true))
        advanceUntilIdle()

        // The active wallet, not the first-created one - the whole point of the fix.
        assertEquals("w_active", vm.state.value.smsImportAccounts[MoneyProviders.ORANGE_MONEY])
    }

    @Test
    fun `enabling import does not overwrite a destination the user already chose`() = runTest(dispatcher) {
        val vm = viewModel(smsProviders = listOf(MoneyProviders.ORANGE_MONEY))
        keepStateHot(vm)
        seedWallet("w_active", "Everyday")
        seedWallet("w_chosen", "Mobile money")
        activeAccountStore.setActiveAccount("w_active")
        advanceUntilIdle()

        vm.onIntent(SettingsIntent.SmsImportAccountChanged(MoneyProviders.ORANGE_MONEY, "w_chosen"))
        advanceUntilIdle()
        vm.onIntent(SettingsIntent.SmsImportEnabledChanged(true))
        advanceUntilIdle()

        assertEquals("w_chosen", vm.state.value.smsImportAccounts[MoneyProviders.ORANGE_MONEY])
    }

    @Test
    fun `changing and clearing a destination flows through to state`() = runTest(dispatcher) {
        val vm = viewModel(smsProviders = listOf(MoneyProviders.ORANGE_MONEY))
        keepStateHot(vm)
        seedWallet("w1", "Everyday")
        advanceUntilIdle()

        vm.onIntent(SettingsIntent.SmsImportAccountChanged(MoneyProviders.ORANGE_MONEY, "w1"))
        advanceUntilIdle()
        assertEquals("w1", vm.state.value.smsImportAccounts[MoneyProviders.ORANGE_MONEY])

        vm.onIntent(SettingsIntent.SmsImportAccountChanged(MoneyProviders.ORANGE_MONEY, null))
        advanceUntilIdle()
        assertEquals(null, vm.state.value.smsImportAccounts[MoneyProviders.ORANGE_MONEY])
    }
}
