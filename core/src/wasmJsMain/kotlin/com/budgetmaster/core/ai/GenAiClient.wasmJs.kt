package com.budgetmaster.core.ai

/**
 * Web has no AI provider wired.
 *
 * Firebase AI Logic exists in the Firebase JS SDK, so this is reachable through Kotlin/Wasm
 * external declarations — but that is real interop work, not a dependency line. Until then AI
 * surfaces hide themselves on the web rather than pretending.
 *
 * The alternative — calling Gemini's REST API from the browser — would put an API key in a wasm
 * bundle anyone can download, which is worse here than anywhere else.
 */
internal class UnavailableGenAiClient : GenAiClient {
    override val isAvailable: Boolean = false

    override suspend fun generateJson(prompt: String, schema: GenAiSchema): String =
        throw GenAiException.Unavailable("Firebase AI Logic is not wired on the web yet")
}

actual fun createGenAiClient(): GenAiClient = UnavailableGenAiClient()
