package com.tsapps.dotbit.backend

data class BackendConfig(
    val port: Int,
    val production: Boolean,
    val backendToken: String?,
    val llmApiKey: String?,
    val llmBaseUrl: String,
    val llmModel: String,
    val allowedOrigins: List<String>,
) {
    companion object {
        fun fromEnvironment(read: (String) -> String? = System::getenv): BackendConfig {
            val production = read("ENVIRONMENT").equals("production", ignoreCase = true)
            val backendToken = read("DOTBIT_BACKEND_TOKEN")?.trim()?.takeIf { it.isNotEmpty() }
            val llmApiKey = read("LLM_API_KEY")?.trim()?.takeIf { it.isNotEmpty() }
            if (production) {
                require(!backendToken.isNullOrBlank()) { "DOTBIT_BACKEND_TOKEN is required in production." }
                require(!llmApiKey.isNullOrBlank()) { "LLM_API_KEY is required in production." }
            }
            return BackendConfig(
                port = read("PORT")?.toIntOrNull()?.takeIf { it in 1..65535 } ?: 8080,
                production = production,
                backendToken = backendToken,
                llmApiKey = llmApiKey,
                llmBaseUrl = read("LLM_BASE_URL")?.trim()?.trimEnd('/')
                    ?.takeIf { it.startsWith("https://") } ?: "https://api.openai.com/v1",
                llmModel = read("LLM_MODEL")?.trim()?.takeIf { it.isNotEmpty() } ?: "gpt-4.1-mini",
                allowedOrigins = read("ALLOWED_ORIGINS")
                    ?.split(',')
                    ?.map(String::trim)
                    ?.filter(String::isNotEmpty)
                    .orEmpty(),
            )
        }
    }
}
