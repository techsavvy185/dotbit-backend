package com.tsapps.dotbit.backend

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
interface CorrectionProvider {
    val name: String
    suspend fun suggest(request: CorrectionRequest): CorrectionResult
}

class DeterministicCorrectionProvider(
    private val policy: CorrectionPolicy,
) : CorrectionProvider {
    override val name = "deterministic-local"
    override suspend fun suggest(request: CorrectionRequest): CorrectionResult = policy.deterministicResult(request)
}

class OpenAiCompatibleCorrectionProvider(
    private val apiKey: String,
    private val baseUrl: String,
    private val model: String,
    private val json: Json,
) : CorrectionProvider {
    override val name = "llm-constrained"

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
    }

    override suspend fun suggest(request: CorrectionRequest): CorrectionResult {
        val payload = ChatCompletionRequest(
            model = model,
            messages = listOf(
                ChatMessage("system", SYSTEM_PROMPT),
                ChatMessage("user", json.encodeToString(CorrectionRequest.serializer(), request)),
            ),
        )
        val response = runCatching {
            client.post("$baseUrl/chat/completions") {
                bearerAuth(apiKey)
                contentType(ContentType.Application.Json)
                setBody(payload)
            }.body<ChatCompletionResponse>()
        }.getOrElse { error ->
            throw ProviderUnavailableException("The correction provider is unavailable: ${error.message}")
        }
        val content = response.choices.firstOrNull()?.message?.content
            ?: throw ProviderUnavailableException("The correction provider returned no result.")
        return runCatching { json.decodeFromString(CorrectionResult.serializer(), content) }
            .getOrElse { throw ProviderPolicyException("The correction provider returned invalid structured JSON.") }
    }

    private companion object {
        val SYSTEM_PROMPT = """
            You are a constrained Braille OCR uncertainty resolver, not an OCR engine and not a grammar checker.
            Return only a JSON CorrectionResult with originalText, correctedText, and corrections.
            You may change only a supplied uncertain span, and a replacement must exactly match one of that span's alternatives.
            Never change confident spelling, punctuation, grammar, or student work.
            Use zero corrections when the uncertainty evidence is insufficient.
            Every correction needs startIndex, endIndex, original, replacement, confidence from 0 to 1, and a short reason tied to OCR uncertainty.
        """.trimIndent()
    }
}
