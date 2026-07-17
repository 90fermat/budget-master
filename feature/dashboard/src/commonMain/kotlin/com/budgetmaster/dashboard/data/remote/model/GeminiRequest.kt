package com.budgetmaster.dashboard.data.remote.model

import kotlinx.serialization.Serializable

/**
 * Request payload representation for the Gemini GenerateContent API.
 */
@Serializable
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    /**
     * Constrains the model's output to a shape the API enforces, instead of asking for JSON in
     * the prompt and hoping. Free text that merely looks like JSON is the usual reason an
     * insights call fails to parse.
     */
    val responseSchema: Schema? = null,
)

/**
 * A subset of the OpenAPI schema dialect Gemini accepts for `responseSchema`.
 *
 * Null fields are omitted by the encoder, so one class covers objects, arrays and primitives
 * without sending empty keys the API would reject.
 */
@Serializable
data class Schema(
    val type: String,
    val description: String? = null,
    val nullable: Boolean? = null,
    val enum: List<String>? = null,
    val items: Schema? = null,
    val properties: Map<String, Schema>? = null,
    val required: List<String>? = null,
)
