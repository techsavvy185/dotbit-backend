package com.tsapps.dotbit.backend

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
class ApplicationTest {
    private val config = BackendConfig(
        port = 8080,
        production = false,
        backendToken = "test-token",
        llmApiKey = null,
        llmBaseUrl = "https://api.example.com/v1",
        llmModel = "test-model",
        allowedOrigins = emptyList(),
    )

    @Test
    fun `health endpoint reports provider`() = testApplication {
        application { module(config) }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `correction endpoint authenticates and returns structured result`() = testApplication {
        application { module(config) }
        val json = Json { ignoreUnknownKeys = false }
        val apiClient = createClient { install(ContentNegotiation) { json(json) } }
        val response = apiClient.post("/v1/corrections") {
            bearerAuth("test-token")
            contentType(ContentType.Application.Json)
            setBody(sampleRequest())
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("The boy played outside.", response.body<CorrectionResult>().correctedText)
    }

    @Test
    fun `unknown image field is rejected`() = testApplication {
        application { module(config) }
        val response = client.post("/v1/corrections") {
            bearerAuth("test-token")
            contentType(ContentType.Application.Json)
            setBody(
                """{"recognizedText":"abc","uncertainSpans":[],"pageImage":"forbidden"}""",
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

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
}
