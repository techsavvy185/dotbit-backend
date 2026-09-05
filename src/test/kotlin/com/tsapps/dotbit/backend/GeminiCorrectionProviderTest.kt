package com.tsapps.dotbit.backend

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class GeminiCorrectionProviderTest {
    private val json = Json {
        ignoreUnknownKeys = false
        explicitNulls = false
        encodeDefaults = true
    }

    @Test
    fun `Gemini structured response is decoded`() = runBlocking {
        val engine = MockEngine { request ->
            assertEquals(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-test:generateContent",
                request.url.toString(),
            )
            assertEquals("secret-key", request.headers["x-goog-api-key"])
            respond(
                content = SUCCESS_RESPONSE,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val provider = provider(HttpClient(engine))

        val result = provider.suggest(sampleRequest())

        assertEquals("The boy played outside.", result.correctedText)
        assertEquals("y", result.corrections.single().replacement)
    }

    @Test
    fun `Gemini HTTP failure is reported without exposing its response body`() = runBlocking {
        val engine = MockEngine {
            respond(
                content = """{"error":{"message":"sensitive provider detail"}}""",
                status = HttpStatusCode.TooManyRequests,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val error = assertFailsWith<ProviderUnavailableException> {
            provider(HttpClient(engine)).suggest(sampleRequest())
        }

        assertTrue(error.message.orEmpty().contains("HTTP 429"))
        assertTrue(!error.message.orEmpty().contains("sensitive provider detail"))
    }

    private fun provider(client: HttpClient) = GeminiCorrectionProvider(
        apiKey = "secret-key",
        baseUrl = "https://generativelanguage.googleapis.com/v1beta",
        modelName = "gemini-test",
        json = json,
        clientOverride = client,
    )

    private fun sampleRequest() = CorrectionRequest(
        recognizedText = "The boy plahed outside.",
        uncertainSpans = listOf(
            UncertainSpan(
                startIndex = 11,
                endIndex = 12,
                original = "h",
                confidence = 0.52f,
                alternatives = listOf(TextCandidate("y", 0.44f)),
            ),
        ),
    )

    private companion object {
        val SUCCESS_RESPONSE = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\"originalText\":\"The boy plahed outside.\",\"correctedText\":\"The boy played outside.\",\"corrections\":[{\"startIndex\":11,\"endIndex\":12,\"original\":\"h\",\"replacement\":\"y\",\"confidence\":0.91,\"reason\":\"The supplied OCR alternative fits the surrounding word.\"}]}"
                      }
                    ],
                    "role": "model"
                  },
                  "finishReason": "STOP"
                }
              ],
              "usageMetadata": {"promptTokenCount": 100}
            }
        """.trimIndent()
    }
}
