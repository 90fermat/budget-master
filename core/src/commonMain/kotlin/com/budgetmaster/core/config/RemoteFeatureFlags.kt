package com.budgetmaster.core.config

/**
 * Server-toggleable feature flags, so a feature can be turned off for everyone without shipping an
 * update — the free-tier kill-switch the AI features want if a quota tightens or a provider
 * misbehaves.
 *
 * Reads are synchronous and safe to call from anywhere: they return the locally-cached value (or
 * the built-in default before the first [refresh]). Only Android is backed by a real remote source
 * today; other platforms return the defaults, which is the correct "no override" behaviour.
 */
interface RemoteFeatureFlags {
    /** The cached value of [key], or [default] if nothing has been fetched or set. */
    fun isEnabled(key: String, default: Boolean = true): Boolean

    /** Fetches the latest values in the background. Failures are swallowed — a flag fetch must
     *  never break app start, and the last-known (or default) values keep working. */
    suspend fun refresh()

    companion object {
        /** Master kill-switch for every AI surface. Defaults on; a project can flip it off. */
        const val AI_FEATURES = "ai_features_enabled"
    }
}

/** The platform's flags source: Firebase Remote Config on Android, defaults-only elsewhere. */
expect fun createRemoteFeatureFlags(): RemoteFeatureFlags
