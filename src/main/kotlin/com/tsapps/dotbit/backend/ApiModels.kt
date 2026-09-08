package com.tsapps.dotbit.backend

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class HealthResponse(
    val status: String,
    val correctionProvider: String,
    val correctionModel: String? = null,
)

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
)

@Serializable
internal data class GeminiGenerateContentRequest(
    val systemInstruction: GeminiContent,
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig = GeminiGenerationConfig(),
)

@Serializable
internal data class GeminiGenerationConfig(
    val maxOutputTokens: Int = 4_096,
    val thinkingConfig: GeminiThinkingConfig = GeminiThinkingConfig(),
    val responseFormat: GeminiResponseFormat = GeminiResponseFormat(),
)

@Serializable
internal data class GeminiThinkingConfig(
    val thinkingLevel: String = "low",
)

@Serializable
internal data class GeminiResponseFormat(
    val text: GeminiTextFormat = GeminiTextFormat(),
)

@Serializable
internal data class GeminiTextFormat(
    val mimeType: String = "application/json",
    val schema: JsonObject = correctionResultSchema,
)

@Serializable
internal data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null,
)

@Serializable
internal data class GeminiPart(val text: String)

@Serializable
internal data class GeminiGenerateContentResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
)

@Serializable
internal data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null,
)

private val correctionResultSchema = buildJsonObject {
    put("type", "object")
    put("additionalProperties", false)
    put("properties", buildJsonObject {
        put("originalText", buildJsonObject { put("type", "string") })
        put("correctedText", buildJsonObject { put("type", "string") })
        put("corrections", buildJsonObject {
            put("type", "array")
            put("items", buildJsonObject {
                put("type", "object")
                put("additionalProperties", false)
                put("properties", buildJsonObject {
                    put("startIndex", buildJsonObject { put("type", "integer") })
                    put("endIndex", buildJsonObject { put("type", "integer") })
                    put("original", buildJsonObject { put("type", "string") })
                    put("replacement", buildJsonObject { put("type", "string") })
                    put("confidence", buildJsonObject {
                        put("type", "number")
                        put("minimum", 0)
                        put("maximum", 1)
                    })
                    put("reason", buildJsonObject { put("type", "string") })
                })
                put("required", buildJsonArray {
                    add("startIndex")
                    add("endIndex")
                    add("original")
                    add("replacement")
                    add("confidence")
                    add("reason")
                })
            })
        })
    })
    put("required", buildJsonArray {
        add("originalText")
        add("correctedText")
        add("corrections")
    })
}
