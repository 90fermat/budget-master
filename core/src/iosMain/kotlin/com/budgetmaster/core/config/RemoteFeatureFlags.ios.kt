package com.budgetmaster.core.config

/**
 * iOS has no Remote Config wired, so every flag is its default — the correct "no server override"
 * behaviour. Wiring the Firebase iOS SDK (Xcode) would make this real.
 */
internal class DefaultRemoteFeatureFlags : RemoteFeatureFlags {
    override fun isEnabled(key: String, default: Boolean): Boolean = default
    override suspend fun refresh() = Unit
}

actual fun createRemoteFeatureFlags(): RemoteFeatureFlags = DefaultRemoteFeatureFlags()
