package com.budgetmaster.core.ai

import com.budgetmaster.core.config.RemoteFeatureFlags

/**
 * A platform text-generation client, with **no business logic** — callers own the prompt and the
 * shape they want back.
 *
 * Exists so the app never holds a model API key. The Android implementation goes through Firebase
 * AI Logic, which proxies Gemini and attests the caller with App Check; the client ships with no
 * secret to extract. Targets without an implementation report [isAvailable] false and the feature
 * hides itself rather than degrading into something broken.
 */
interface GenAiClient {

    /**
     * Whether generation can actually run on this platform and build.
     *
     * False on targets with no SDK wired. Callers must check it rather than calling and handling
     * the failure — an AI surface that can never fill should not be shown at all.
     */
    val isAvailable: Boolean

    /**
     * Generates JSON matching [schema].
     *
     * @param prompt The full prompt. Callers are responsible for what it contains; nothing here
     *   inspects or rewrites it.
     * @param schema The shape the response must take, enforced by the provider rather than
     *   requested in prose.
     * @return The raw JSON text.
     * @throws GenAiException if generation fails or the provider is unavailable.
     */
    suspend fun generateJson(prompt: String, schema: GenAiSchema): String
}

/**
 * Why a generation attempt failed, in terms a caller can act on.
 *
 * [RateLimited] is worth retrying; the others are not — the same call will fail the same way.
 */
sealed class GenAiException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** The provider is rate-limiting. Transient: back off and retry. */
    class RateLimited(cause: Throwable? = null) : GenAiException("Rate limited", cause)

    /** No provider on this platform/build. */
    class Unavailable(message: String = "No AI provider is configured") : GenAiException(message)

    /**
     * The backend refused to attest this app instance (App Check).
     *
     * Separate from [Failed] because retrying cannot help and the fix is not the user's: a debug
     * build needs its debug token registered in the Firebase console, and a release build needs
     * to have come from Play. Bucketing it with network errors is what made a 97% rejection rate
     * invisible - the UI said "could not reach the AI, try again in a moment" and users obligingly
     * retried into the same refusal.
     */
    class NotAuthorized(message: String, cause: Throwable? = null) : GenAiException(message, cause)

    /** Anything else: network, malformed response. */
    class Failed(message: String, cause: Throwable? = null) : GenAiException(message, cause)
}

/**
 * A provider-agnostic response schema.
 *
 * Deliberately a small subset — enough to describe the shapes this app asks for, without leaking
 * a vendor's schema type into `:core`'s API or into feature modules.
 */
sealed interface GenAiSchema {
    /** A free-text string. */
    data class Str(val description: String? = null) : GenAiSchema

    /** A string constrained to [values], so the model cannot invent one. */
    data class Enumeration(val values: List<String>) : GenAiSchema

    /** A list of [items]. */
    data class Arr(val items: GenAiSchema) : GenAiSchema

    /**
     * An object.
     *
     * @param properties Field name to shape.
     * @param optional Names within [properties] the model may omit; everything else is required.
     */
    data class Obj(
        val properties: Map<String, GenAiSchema>,
        val optional: List<String> = emptyList(),
    ) : GenAiSchema
}

/**
 * The platform's client.
 *
 * An expect *function* rather than an expect class: the Android implementation needs constructor
 * arguments the other targets have no concept of, and callers only ever want "the one for this
 * platform".
 *
 * @param flags the remote kill-switch. The Android client reports [GenAiClient.isAvailable] false
 *   when AI is remotely disabled, so every AI surface goes dark without per-feature wiring.
 */
expect fun createGenAiClient(flags: RemoteFeatureFlags): GenAiClient
