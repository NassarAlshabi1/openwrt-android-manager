package com.luanmuc.openwrtmanager.data.model

import com.google.gson.annotations.SerializedName

/**
 * Gemini API request/response models.
 *
 * Used by the AI diagnostic feature to talk to
 * generativelanguage.googleapis.com (v1beta generateContent).
 */

// ==================== Request ====================

data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerializedName("systemInstruction")
    val systemInstruction: GeminiContent? = null,
    @SerializedName("generationConfig")
    val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    val role: String, // "user" or "model"
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

data class GeminiGenerationConfig(
    val temperature: Double = 0.4,
    val maxOutputTokens: Int = 4096
)

// ==================== Response ====================

data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiError? = null
)

data class GeminiCandidate(
    val content: GeminiContent? = null,
    @SerializedName("finishReason")
    val finishReason: String? = null
)

data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
) {
    /**
     * Human-readable error text, localized when possible.
     */
    fun describe(): String {
        return when (code) {
            400 -> if (status == "INVALID_ARGUMENT") "Invalid API key" else (message ?: "Bad request")
            401, 403 -> "API key rejected (401/403)"
            429 -> "Rate limit exceeded (429) — try again later"
            else -> message ?: "Gemini error $code"
        }
    }
}
