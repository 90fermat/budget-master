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
    val responseMimeType: String? = null
)
