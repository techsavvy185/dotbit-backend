package com.tsapps.dotbit.backend

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
class CorrectionPolicyTest {
    private val policy = CorrectionPolicy()

    @Test
    fun `deterministic provider changes only a supplied uncertain candidate`() {
        val request = sampleRequest()
        val result = policy.deterministicResult(request)

        assertEquals("The boy played outside.", result.correctedText)
        assertEquals(1, result.corrections.size)
        policy.validateResult(request, result)
    }

    @Test
    fun `provider response outside uncertainty is rejected`() {
        val request = sampleRequest()
        val unsafe = CorrectionResult(
            originalText = request.recognizedText,
            correctedText = "Ahe boy plahed outside.",
            corrections = listOf(
                TextCorrection(0, 1, "T", "A", 0.9f, "Grammar"),
            ),
        )

        assertFailsWith<ProviderPolicyException> { policy.validateResult(request, unsafe) }
    }

    @Test
    fun `span must match recognized text`() {
        val invalid = sampleRequest().copy(
            uncertainSpans = listOf(sampleRequest().uncertainSpans.single().copy(original = "x")),
        )

        assertFailsWith<RequestValidationException> { policy.validateRequest(invalid) }
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
