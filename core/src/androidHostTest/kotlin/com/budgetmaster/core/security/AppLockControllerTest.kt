@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.budgetmaster.core.security

import com.budgetmaster.core.prefs.AppSettingsRepository
import com.budgetmaster.core.prefs.KeyValueStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FakeStore : KeyValueStore {
    private val entries = MutableStateFlow<Map<String, String>>(emptyMap())
    override fun observeString(key: String): Flow<String?> = entries.map { it[key] }
    override suspend fun putString(key: String, value: String) { entries.value = entries.value + (key to value) }
    override suspend fun remove(key: String) { entries.value = entries.value - key }
}

class AppLockControllerTest {

    private var clock = 0L

    private fun controller(scope: TestScope, repo: AppSettingsRepository) =
        AppLockController(repo, scope.backgroundScope, now = { clock })

    @Test
    fun `disabled lock stays unlocked from cold start`() = runTest(UnconfinedTestDispatcher()) {
        val repo = AppSettingsRepository(FakeStore())
        val subject = controller(this, repo)
        advanceUntilIdle()
        assertFalse(subject.isLocked.value)
    }

    @Test
    fun `enabled lock is locked at cold start`() = runTest(UnconfinedTestDispatcher()) {
        val store = FakeStore()
        val repo = AppSettingsRepository(store)
        repo.setAppLockEnabled(true)
        val subject = controller(this, repo)
        advanceUntilIdle()
        assertTrue(subject.isLocked.value)
    }

    @Test
    fun `turning the lock off unlocks a running app`() = runTest(UnconfinedTestDispatcher()) {
        val repo = AppSettingsRepository(FakeStore())
        repo.setAppLockEnabled(true)
        val subject = controller(this, repo)
        advanceUntilIdle()
        assertTrue(subject.isLocked.value)

        repo.setAppLockEnabled(false)
        advanceUntilIdle()
        assertFalse(subject.isLocked.value)
    }

    @Test
    fun `backgrounding past the timeout re-locks on return`() = runTest(UnconfinedTestDispatcher()) {
        val repo = AppSettingsRepository(FakeStore())
        repo.setAppLockEnabled(true)
        repo.setAppLockTimeoutSeconds(60)
        val subject = controller(this, repo)
        advanceUntilIdle()
        subject.unlockWithBiometric()
        assertFalse(subject.isLocked.value)

        subject.onMovedToBackground()
        clock += 61_000
        subject.onMovedToForeground()
        assertTrue(subject.isLocked.value, "60s grace exceeded, should re-lock")
    }

    @Test
    fun `backgrounding within the timeout does not re-lock`() = runTest(UnconfinedTestDispatcher()) {
        val repo = AppSettingsRepository(FakeStore())
        repo.setAppLockEnabled(true)
        repo.setAppLockTimeoutSeconds(60)
        val subject = controller(this, repo)
        advanceUntilIdle()
        subject.unlockWithBiometric()

        subject.onMovedToBackground()
        clock += 10_000
        subject.onMovedToForeground()
        assertFalse(subject.isLocked.value, "within the 60s grace, stays unlocked")
    }

    @Test
    fun `correct pin unlocks, wrong pin does not`() = runTest(UnconfinedTestDispatcher()) {
        val repo = AppSettingsRepository(FakeStore())
        repo.setAppLockEnabled(true)
        repo.setAppLockPinHash(PinHasher.hash("2468"))
        val subject = controller(this, repo)
        advanceUntilIdle()

        assertIs<PinResult.Wrong>(subject.submitPin("0000"))
        assertTrue(subject.isLocked.value)
        assertIs<PinResult.Success>(subject.submitPin("2468"))
        assertFalse(subject.isLocked.value)
    }

    @Test
    fun `five wrong attempts trigger a lockout`() = runTest(UnconfinedTestDispatcher()) {
        val repo = AppSettingsRepository(FakeStore())
        repo.setAppLockEnabled(true)
        repo.setAppLockPinHash(PinHasher.hash("2468"))
        val subject = controller(this, repo)
        advanceUntilIdle()

        repeat(4) { assertIs<PinResult.Wrong>(subject.submitPin("0000")) }
        val locked = subject.submitPin("0000")
        assertIs<PinResult.LockedOut>(locked)
        assertEquals(30L, locked.secondsRemaining)

        // Even the correct PIN is refused while locked out.
        assertIs<PinResult.LockedOut>(subject.submitPin("2468"))

        // After the window passes, the correct PIN works again.
        clock += 31_000
        assertIs<PinResult.Success>(subject.submitPin("2468"))
    }
}
