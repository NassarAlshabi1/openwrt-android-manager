package com.luanmuc.openwrtmanager.data.api

import com.luanmuc.openwrtmanager.data.model.GeminiRequest
import com.luanmuc.openwrtmanager.data.model.GeminiResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Dedicated Retrofit client for the Google Gemini API.
 *
 * Kept separate from [RetrofitClient] (router traffic) on purpose:
 * - Always HTTPS to generativelanguage.googleapis.com
 * - No trust-all SSL manager
 * - Longer read timeout for LLM generation
 */
object GeminiClient {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    /** Default model — fast, multimodal, widely available. */
    const val DEFAULT_MODEL = "gemini-2.0-flash"

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            // BASIC only: never log request bodies or the API key
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("x-goog-api-key")
        }

        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS) // LLM answers can take a while
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: GeminiApiService by lazy {
        retrofit.create(GeminiApiService::class.java)
    }

    /**
     * One-shot helper: send a request and extract the answer text.
     * @throws GeminiException when the API returns an error or empty answer
     */
    suspend fun ask(model: String, apiKey: String, request: GeminiRequest): String {
        val response = try {
            apiService.generateContent(model, apiKey, request)
        } catch (e: Exception) {
            throw GeminiException(e.message ?: "network error", cause = e)
        }
        response.error?.let { throw GeminiException(it.describe()) }
        val text = response.candidates
            ?.firstOrNull()
            ?.content?.parts
            ?.mapNotNull { it.text }
            ?.joinToString("") { it }
            ?.trim()
        if (text.isNullOrEmpty()) {
            throw GeminiException("empty response from Gemini")
        }
        return text
    }

    class GeminiException(message: String, cause: Throwable? = null) : Exception(message, cause)
}
