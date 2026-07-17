package com.budgetmaster.core.config

/**
 * Web has no Remote Config wired, so every flag is its default — the correct "no server override"
 * behaviour. It would come through the Firebase JS SDK if that were bridged.
 */
internal class DefaultRemoteFeatureFlags : RemoteFeatureFlags {
    override fun isEnabled(key: String, default: Boolean): Boolean = default
    override suspend fun refresh() = Unit
}

actual fun createRemoteFeatureFlags(): RemoteFeatureFlags = DefaultRemoteFeatureFlags()
