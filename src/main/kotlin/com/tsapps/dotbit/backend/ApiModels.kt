package com.tsapps.dotbit.backend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val correctionProvider: String,
)

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
)

@Serializable
internal data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.0,
    @SerialName("response_format") val responseFormat: ResponseFormat = ResponseFormat(),
)

@Serializable
internal data class ResponseFormat(val type: String = "json_object")

@Serializable
internal data class ChatMessage(val role: String, val content: String)

@Serializable
internal data class ChatCompletionResponse(val choices: List<ChatChoice>)

@Serializable
internal data class ChatChoice(val message: ChatMessage)
