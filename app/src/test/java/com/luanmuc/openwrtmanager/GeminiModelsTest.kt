package com.luanmuc.openwrtmanager

import com.google.gson.Gson
import com.luanmuc.openwrtmanager.data.model.GeminiContent
import com.luanmuc.openwrtmanager.data.model.GeminiError
import com.luanmuc.openwrtmanager.data.model.GeminiGenerationConfig
import com.luanmuc.openwrtmanager.data.model.GeminiPart
import com.luanmuc.openwrtmanager.data.model.GeminiRequest
import com.luanmuc.openwrtmanager.data.model.GeminiResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the Gemini API request/response models used by the
 * AI diagnostic feature.
 */
class GeminiModelsTest {

    private val gson = Gson()

    // ---------- Request serialization ----------

    @Test
    fun `request serializes contents and systemInstruction`() {
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(role = "user", parts = listOf(GeminiPart("hello")))
            ),
            systemInstruction = GeminiContent(role = "system", parts = listOf(GeminiPart("be brief"))),
            generationConfig = GeminiGenerationConfig(temperature = 0.3, maxOutputTokens = 128)
        )

        val json = gson.toJson(request)

        assertTrue(json.contains("\"contents\""))
        assertTrue(json.contains("\"systemInstruction\""))
        assertTrue(json.contains("\"generationConfig\""))
        assertTrue(json.contains("\"temperature\":0.3"))
        assertTrue(json.contains("\"maxOutputTokens\":128"))
        assertTrue(json.contains("\"role\":\"user\""))
        assertTrue(json.contains("\"text\":\"hello\""))
    }

    @Test
    fun `generationConfig defaults are applied`() {
        val config = GeminiGenerationConfig()
        assertEquals(0.4, config.temperature, 0.0001)
        assertEquals(4096, config.maxOutputTokens)
    }

    // ---------- Response parsing (real Gemini payload shape) ----------

    @Test
    fun `parses a successful generateContent response`() {
        val json = """
        {
          "candidates": [
            {
              "content": {
                "parts": [
                  {"text": "Verdict: router looks healthy."},
                  {"text": " See suggestions below."}
                ],
                "role": "model"
              },
              "finishReason": "STOP"
            }
          ],
          "usageMetadata": {"promptTokenCount": 100, "candidatesTokenCount": 42}
        }
        """.trimIndent()

        val response = gson.fromJson(json, GeminiResponse::class.java)

        assertNull(response.error)
        assertNotNull(response.candidates)
        assertEquals(1, response.candidates!!.size)
        val text = response.candidates!![0].content?.parts
            ?.mapNotNull { it.text }
            ?.joinToString("") { it }
        assertEquals("Verdict: router looks healthy. See suggestions below.", text)
        assertEquals("STOP", response.candidates!![0].finishReason)
    }

    @Test
    fun `parses an error payload`() {
        val json = """
        {
          "error": {
            "code": 429,
            "message": "Resource has been exhausted",
            "status": "RESOURCE_EXHAUSTED"
          }
        }
        """.trimIndent()

        val response = gson.fromJson(json, GeminiResponse::class.java)

        assertNotNull(response.error)
        assertEquals(429, response.error!!.code)
        assertEquals("RESOURCE_EXHAUSTED", response.error!!.status)
        assertTrue(response.error!!.describe().contains("429"))
    }

    @Test
    fun `error describe maps auth failures`() {
        val invalidKey = GeminiError(code = 400, message = "API key not valid", status = "INVALID_ARGUMENT")
        assertEquals("Invalid API key", invalidKey.describe())

        val forbidden = GeminiError(code = 403, message = "Permission denied", status = "PERMISSION_DENIED")
        assertEquals("API key rejected (401/403)", forbidden.describe())
    }

    @Test
    fun `empty candidates list is handled`() {
        val json = """{"candidates": []}"""
        val response = gson.fromJson(json, GeminiResponse::class.java)
        assertTrue(response.candidates?.isEmpty() == true)
        assertNull(response.error)
    }
}
