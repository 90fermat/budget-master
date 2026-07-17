package com.budgetmaster.core.ai

/**
 * iOS has no AI provider wired.
 *
 * Firebase AI Logic ships an Android library and a Swift package; there is no Kotlin
 * Multiplatform wrapper, so using it from here means adding the Swift SDK in Xcode and bridging
 * it — which needs macOS. Until then AI surfaces hide themselves on iOS rather than pretending.
 *
 * The alternative — calling Gemini's REST API directly from iOS — is what was just removed: it
 * requires an embedded API key.
 */
internal class UnavailableGenAiClient : GenAiClient {
    override val isAvailable: Boolean = false

    override suspend fun generateJson(prompt: String, schema: GenAiSchema): String =
        throw GenAiException.Unavailable("Firebase AI Logic is not wired on iOS yet")
}

actual fun createGenAiClient(): GenAiClient = UnavailableGenAiClient()
