package com.luanmuc.openwrtmanager.data.api

import com.luanmuc.openwrtmanager.data.model.GeminiRequest
import com.luanmuc.openwrtmanager.data.model.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Gemini REST API (v1beta) interface.
 *
 * The API key is sent via the `x-goog-api-key` header so it never
 * appears in URLs/logs. A dedicated Retrofit instance (plain OkHttp,
 * no router trust-all manager) is created in [GeminiClient].
 */
interface GeminiApiService {

    /**
     * Generate content with the given model, e.g. "gemini-2.0-flash".
     */
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body body: GeminiRequest
    ): GeminiResponse
}
