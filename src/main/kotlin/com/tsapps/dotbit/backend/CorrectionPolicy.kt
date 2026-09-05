package com.tsapps.dotbit.backend

class CorrectionPolicy {
    fun validateRequest(request: CorrectionRequest) {
        requireValid(request.recognizedText.isNotBlank(), "recognizedText must not be blank.")
        requireValid(request.recognizedText.length <= MAX_TEXT_LENGTH, "recognizedText is too long.")
        requireValid(request.uncertainSpans.size <= MAX_UNCERTAIN_SPANS, "Too many uncertain spans.")

        var previousEnd = 0
        request.uncertainSpans.sortedBy { it.startIndex }.forEach { span ->
            requireValid(span.startIndex in 0 until request.recognizedText.length, "Invalid span startIndex.")
            requireValid(span.endIndex in (span.startIndex + 1)..request.recognizedText.length, "Invalid span endIndex.")
            requireValid(span.startIndex >= previousEnd, "Uncertain spans must not overlap.")
            requireValid(span.confidence in 0f..1f, "Span confidence must be between 0 and 1.")
            requireValid(
                request.recognizedText.substring(span.startIndex, span.endIndex) == span.original,
                "Span original does not match recognizedText.",
            )
            requireValid(span.alternatives.size <= MAX_ALTERNATIVES, "Too many alternatives for a span.")
            span.alternatives.forEach { candidate ->
                requireValid(candidate.text.isNotEmpty(), "Alternative text must not be empty.")
                requireValid(candidate.confidence in 0f..1f, "Alternative confidence must be between 0 and 1.")
            }
            previousEnd = span.endIndex
        }
    }

    fun validateResult(request: CorrectionRequest, result: CorrectionResult): CorrectionResult {
        requireProvider(result.originalText == request.recognizedText, "Provider changed originalText.")
        val spansByRange = request.uncertainSpans.associateBy { it.startIndex to it.endIndex }
        val ordered = result.corrections.sortedBy { it.startIndex }
        var previousEnd = 0
        ordered.forEach { correction ->
            val span = spansByRange[correction.startIndex to correction.endIndex]
                ?: throw ProviderPolicyException("Correction is outside an uncertain span.")
            requireProvider(correction.startIndex >= previousEnd, "Corrections overlap.")
            requireProvider(correction.original == span.original, "Correction original does not match its span.")
            requireProvider(correction.replacement in span.alternatives.map { it.text }, "Replacement is not an OCR candidate.")
            requireProvider(correction.confidence in 0f..1f, "Correction confidence must be between 0 and 1.")
            requireProvider(correction.reason.isNotBlank(), "Correction reason is required.")
            previousEnd = correction.endIndex
        }

        val reconstructed = applyCorrections(request.recognizedText, ordered)
        requireProvider(reconstructed == result.correctedText, "correctedText does not match corrections.")
        return result.copy(corrections = ordered)
    }

    fun deterministicResult(request: CorrectionRequest): CorrectionResult {
        val corrections = request.uncertainSpans.mapNotNull { span ->
            val candidate = span.alternatives
                .filter { it.text != span.original }
                .maxByOrNull { it.confidence }
                ?.takeIf { it.confidence >= MIN_DETERMINISTIC_CONFIDENCE }
                ?: return@mapNotNull null
            TextCorrection(
                startIndex = span.startIndex,
                endIndex = span.endIndex,
                original = span.original,
                replacement = candidate.text,
                confidence = candidate.confidence,
                reason = "Selected from the supplied alternatives for this uncertain OCR span.",
            )
        }
        return CorrectionResult(
            originalText = request.recognizedText,
            correctedText = applyCorrections(request.recognizedText, corrections),
            corrections = corrections,
        )
    }

    private fun applyCorrections(original: String, corrections: List<TextCorrection>): String {
        var output = original
        corrections.sortedByDescending { it.startIndex }.forEach { correction ->
            output = output.replaceRange(correction.startIndex, correction.endIndex, correction.replacement)
        }
        return output
    }

    private fun requireValid(condition: Boolean, message: String) {
        if (!condition) throw RequestValidationException(message)
    }

    private fun requireProvider(condition: Boolean, message: String) {
        if (!condition) throw ProviderPolicyException(message)
    }

    private companion object {
        const val MAX_TEXT_LENGTH = 20_000
        const val MAX_UNCERTAIN_SPANS = 200
        const val MAX_ALTERNATIVES = 10
        const val MIN_DETERMINISTIC_CONFIDENCE = 0.35f
    }
}

class RequestValidationException(message: String) : IllegalArgumentException(message)
class ProviderPolicyException(message: String) : IllegalStateException(message)
class ProviderUnavailableException(message: String) : IllegalStateException(message)
