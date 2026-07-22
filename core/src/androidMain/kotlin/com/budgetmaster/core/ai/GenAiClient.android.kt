package com.budgetmaster.core.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.FirebaseAIException
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseStoppedException
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.generationConfig
import com.budgetmaster.core.config.RemoteFeatureFlags

/**
 * Android generation via **Firebase AI Logic**.
 *
 * Firebase proxies the call to Gemini and attests it with App Check, so no API key exists in the
 * app. This replaced a direct REST call that carried an embedded key — extractable from any APK,
 * which meant release builds had to ship with the feature disabled.
 *
 * The model is built per call: it is cheap, and a schema is per-request.
 */
internal class FirebaseGenAiClient(
    private val flags: RemoteFeatureFlags,
) : GenAiClient {

    // A provider exists, but a remote kill-switch can still take every AI surface dark — a quota
    // scare or a misbehaving model shouldn't need an app update to disable.
    override val isAvailable: Boolean
        get() = flags.isEnabled(RemoteFeatureFlags.AI_FEATURES, default = true)

    override suspend fun generateJson(prompt: String, schema: GenAiSchema): String {
        val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema.toFirebaseSchema()
            },
        )

        return try {
            model.generateContent(prompt).text
                ?: throw GenAiException.Failed("Empty response")
        } catch (e: QuotaExceededException) {
            // The one worth retrying; everything below fails the same however long you wait.
            throw GenAiException.RateLimited(e)
        } catch (e: PromptBlockedException) {
            throw GenAiException.Failed("Prompt blocked by safety filters", e)
        } catch (e: ResponseStoppedException) {
            throw GenAiException.Failed("Response stopped: ${e.response.candidates.firstOrNull()?.finishReason}", e)
        } catch (e: ServerException) {
            // App Check rejections arrive here as a server error mentioning the token. They are
            // not transient and must not be reported as "try again".
            if (e.isAppCheckRejection()) {
                throw GenAiException.NotAuthorized("App Check rejected this app instance", e)
            }
            throw GenAiException.Failed("AI Logic server error: ${e.message}", e)
        } catch (e: FirebaseAIException) {
            // Deliberate catch-all for the SDK's own hierarchy. Without it, any subtype not named
            // above escaped `generateJson` un-wrapped, past callers that only catch GenAiException
            // - which is how an App Check refusal could surface as an unhandled failure.
            if (e.isAppCheckRejection()) {
                throw GenAiException.NotAuthorized("App Check rejected this app instance", e)
            }
            throw GenAiException.Failed(e.message ?: "AI Logic call failed", e)
        }
    }

    /**
     * Whether this failure is the backend refusing to attest the app.
     *
     * Matched on the message because Firebase AI Logic surfaces App Check refusals as a generic
     * server error rather than a distinct type. Narrow and case-insensitive; a miss only costs the
     * clearer message, never correctness.
     */
    private fun Throwable.isAppCheckRejection(): Boolean {
        val text = message?.lowercase() ?: return false
        return "app check" in text || "appcheck" in text
    }

    private companion object {
        const val MODEL_NAME = "gemini-3.5-flash"
    }
}

/** Maps the provider-agnostic schema onto Firebase's, which is why `:core`'s API exposes neither. */
private fun GenAiSchema.toFirebaseSchema(): Schema = when (this) {
    is GenAiSchema.Str -> Schema.string(description = description)
    is GenAiSchema.Enumeration -> Schema.enumeration(values)
    is GenAiSchema.Arr -> Schema.array(items.toFirebaseSchema())
    is GenAiSchema.Obj -> Schema.obj(
        properties = properties.mapValues { (_, value) -> value.toFirebaseSchema() },
        optionalProperties = optional,
    )
}

actual fun createGenAiClient(flags: RemoteFeatureFlags): GenAiClient = FirebaseGenAiClient(flags)
