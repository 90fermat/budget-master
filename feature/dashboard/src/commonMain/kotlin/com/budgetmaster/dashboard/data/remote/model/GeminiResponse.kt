package com.budgetmaster.dashboard.data.remote.model

import kotlinx.serialization.Serializable

/**
 * Response payload representation for the Gemini GenerateContent API.
 */
@Serializable
data class GeminiResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: ResponseContent? = null
)

@Serializable
data class ResponseContent(
    val parts: List<ResponsePart>? = null
)

@Serializable
data class ResponsePart(
    val text: String? = null
)

/**
 * The inner JSON array elements that Gemini is instructed to output.
 */
@Serializable
data class GeminiInsightDto(
    val type: String,
    val message: String,
    val actionRoute: String? = null
)
