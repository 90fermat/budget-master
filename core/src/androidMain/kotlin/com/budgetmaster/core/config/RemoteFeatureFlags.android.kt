package com.budgetmaster.core.config

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.tasks.await

/**
 * Android flags via Firebase Remote Config.
 *
 * `getBoolean` reads the locally-cached, already-activated value synchronously, so callers never
 * block on the network. In-app defaults are set here so a flag is meaningful on the very first
 * launch, before any fetch — and so nothing is ever accidentally *off* by default.
 */
internal class FirebaseRemoteFeatureFlags : RemoteFeatureFlags {

    private val remoteConfig = Firebase.remoteConfig.apply {
        setDefaultsAsync(
            mapOf(RemoteFeatureFlags.AI_FEATURES to true),
        )
    }

    override fun isEnabled(key: String, default: Boolean): Boolean =
        // getBoolean returns false for an unknown key, so fall back to the caller's default there.
        if (remoteConfig.getValue(key).source == com.google.firebase.remoteconfig.FirebaseRemoteConfig.VALUE_SOURCE_STATIC) {
            default
        } else {
            remoteConfig.getBoolean(key)
        }

    override suspend fun refresh() {
        try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            // A flag refresh must never break app start; keep the last-known/default values.
        }
    }
}

actual fun createRemoteFeatureFlags(): RemoteFeatureFlags = FirebaseRemoteFeatureFlags()
