package com.tsapps.dotbit.backend

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BackendConfigTest {
    @Test
    fun `production requires a Gemini key`() {
        val environment = mapOf(
            "ENVIRONMENT" to "production",
            "DOTBIT_BACKEND_TOKEN" to "backend-token",
        )

        assertFailsWith<IllegalArgumentException> {
            BackendConfig.fromEnvironment(environment::get)
        }
    }

    @Test
    fun `Gemini configuration uses stable defaults`() {
        val environment = mapOf(
            "GEMINI_API_KEY" to "gemini-key",
        )

        val config = BackendConfig.fromEnvironment(environment::get)

        assertEquals("gemini-key", config.geminiApiKey)
        assertEquals("https://generativelanguage.googleapis.com/v1beta", config.geminiBaseUrl)
        assertEquals("gemini-3.8-flash", config.geminiModel)
    }
}
