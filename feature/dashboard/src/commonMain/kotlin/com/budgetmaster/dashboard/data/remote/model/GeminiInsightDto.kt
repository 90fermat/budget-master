package com.budgetmaster.dashboard.data.remote.model

import kotlinx.serialization.Serializable

/**
 * One element of the JSON array the model is asked to produce.
 *
 * The request/response envelope types that used to live alongside this are gone: the app no
 * longer speaks the GenerateContent REST protocol itself — `core.ai.GenAiClient` (Firebase AI
 * Logic) hands back the generated text, and this is the only shape still worth parsing.
 */
@Serializable
data class GeminiInsightDto(
    val type: String,
    val message: String,
    val actionRoute: String? = null
)
