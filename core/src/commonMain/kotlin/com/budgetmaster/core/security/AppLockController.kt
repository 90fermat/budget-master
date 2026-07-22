@file:OptIn(ExperimentalTime::class)

package com.budgetmaster.core.security

import com.budgetmaster.core.prefs.AppSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** The result of a PIN attempt, so the UI can tell "wrong" from "locked out" apart. */
sealed interface PinResult {
    data object Success : PinResult
    data object Wrong : PinResult

    /** Too many wrong attempts; retry is blocked for [secondsRemaining]. */
    data class LockedOut(val secondsRemaining: Long) : PinResult
}

/**
 * Owns whether the app is currently locked, and enforces the unlock rules.
 *
 * A single instance, held for the process, so the lock state survives screen and activity
 * recreation — the gate observes [isLocked]. Settings are cached from a collected flow so the
 * lifecycle callbacks ([onMovedToBackground] / [onMovedToForeground]) stay non-suspending, which
 * is what the platform lifecycle hooks can actually call.
 *
 * Rate limiting lives here rather than in the screen because it must survive the screen: closing
 * and reopening the lock UI must not reset the failed-attempt count, or the limit is no limit.
 */
class AppLockController(
    private val settingsRepository: AppSettingsRepository,
    scope: CoroutineScope,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var enabled = false
    private var timeoutSeconds = 0
    private var initialized = false

    private var backgroundedAt: Long? = null
    private var failedAttempts = 0
    private var lockoutUntil: Long = 0L

    init {
        settingsRepository.settings
            .onEach { settings ->
                enabled = settings.appLockEnabled
                timeoutSeconds = settings.appLockTimeoutSeconds
                // Cold start: the first time we learn the setting, lock if it is on. Later setting
                // changes never *lock* a running, already-unlocked app - only enabling it fresh at
                // the next launch should - but turning the lock off unlocks immediately.
                if (!initialized) {
                    initialized = true
                    if (enabled) _isLocked.value = true
                } else if (!enabled) {
                    _isLocked.value = false
                }
            }
            .launchIn(scope)
    }

    /** Records when the app left the foreground, to measure the grace period against. */
    fun onMovedToBackground() {
        backgroundedAt = now()
    }

    /** Re-locks if the app was in the background past the grace period. */
    fun onMovedToForeground() {
        if (!enabled) return
        val since = backgroundedAt ?: return
        if (now() - since >= timeoutSeconds * 1000L) {
            _isLocked.value = true
        }
    }

    /** Called after a successful biometric unlock. */
    fun unlockWithBiometric() {
        failedAttempts = 0
        _isLocked.value = false
    }

    /**
     * Checks [pin] against the stored hash and, on success, unlocks.
     *
     * Applies a widening lockout after repeated failures so the tiny PIN keyspace cannot be
     * walked online: brute force is bounded to a handful of tries per lockout window.
     */
    suspend fun submitPin(pin: String): PinResult {
        val remaining = lockoutRemainingSeconds()
        if (remaining > 0) return PinResult.LockedOut(remaining)

        val hash = settingsRepository.settings.first().appLockPinHash
        if (hash != null && PinHasher.verify(pin, hash)) {
            failedAttempts = 0
            _isLocked.value = false
            return PinResult.Success
        }

        failedAttempts++
        if (failedAttempts >= ATTEMPTS_BEFORE_LOCKOUT) {
            // 30s, then 60s, then 120s… capped, resetting once a correct PIN gets through.
            val steps = failedAttempts - ATTEMPTS_BEFORE_LOCKOUT
            val backoff = (BASE_LOCKOUT_SECONDS shl steps.coerceAtMost(MAX_BACKOFF_SHIFT))
                .coerceAtMost(MAX_LOCKOUT_SECONDS)
            lockoutUntil = now() + backoff * 1000L
            return PinResult.LockedOut(backoff)
        }
        return PinResult.Wrong
    }

    /** Seconds until PIN entry is allowed again, or 0 if it is allowed now. */
    fun lockoutRemainingSeconds(): Long {
        val remainingMs = lockoutUntil - now()
        return if (remainingMs > 0) (remainingMs + 999) / 1000 else 0
    }

    private companion object {
        const val ATTEMPTS_BEFORE_LOCKOUT = 5
        const val BASE_LOCKOUT_SECONDS = 30L
        const val MAX_LOCKOUT_SECONDS = 300L
        const val MAX_BACKOFF_SHIFT = 4
    }
}
