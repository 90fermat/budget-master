package com.budgetmaster.core.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseStoppedException
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.generationConfig

/**
 * Android generation via **Firebase AI Logic**.
 *
 * Firebase proxies the call to Gemini and attests it with App Check, so no API key exists in the
 * app. This replaced a direct REST call that carried an embedded key — extractable from any APK,
 * which meant release builds had to ship with the feature disabled.
 *
 * The model is built per call: it is cheap, and a schema is per-request.
 */
internal class FirebaseGenAiClient : GenAiClient {

    override val isAvailable: Boolean = true

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
            throw GenAiException.Failed("AI Logic server error: ${e.message}", e)
        }
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

actual fun createGenAiClient(): GenAiClient = FirebaseGenAiClient()
