package com.tsapps.dotbit.backend

import kotlinx.serialization.Serializable

@Serializable
data class CorrectionRequest(
    val recognizedText: String,
    val uncertainSpans: List<UncertainSpan>,
)

@Serializable
data class UncertainSpan(
    val startIndex: Int,
    val endIndex: Int,
    val original: String,
    val confidence: Float,
    val alternatives: List<TextCandidate>,
)

@Serializable
data class TextCandidate(
    val text: String,
    val confidence: Float,
)

@Serializable
data class CorrectionResult(
    val originalText: String,
    val correctedText: String,
    val corrections: List<TextCorrection>,
)

@Serializable
data class TextCorrection(
    val startIndex: Int,
    val endIndex: Int,
    val original: String,
    val replacement: String,
    val confidence: Float,
    val reason: String,
)
