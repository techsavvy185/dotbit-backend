package com.tsapps.dotbit.backend

data class BackendConfig(
    val port: Int,
    val production: Boolean,
    val backendToken: String?,
    val geminiApiKey: String?,
    val geminiBaseUrl: String,
    val geminiModel: String,
    val allowedOrigins: List<String>,
) {
    companion object {
        fun fromEnvironment(read: (String) -> String? = System::getenv): BackendConfig {
            val production = read("ENVIRONMENT").equals("production", ignoreCase = true)
            val backendToken = read("DOTBIT_BACKEND_TOKEN")?.trim()?.takeIf { it.isNotEmpty() }
            val geminiApiKey = read("GEMINI_API_KEY")?.trim()?.takeIf { it.isNotEmpty() }
            if (production) {
                require(!backendToken.isNullOrBlank()) { "DOTBIT_BACKEND_TOKEN is required in production." }
                require(!geminiApiKey.isNullOrBlank()) { "GEMINI_API_KEY is required in production." }
            }
            val geminiModel = read("GEMINI_MODEL")
                ?.trim()
                ?.removePrefix("models/")
                ?.takeIf { it.matches(Regex("[A-Za-z0-9._-]+")) }
                ?: "gemini-3.8-flash"
            return BackendConfig(
                port = read("PORT")?.toIntOrNull()?.takeIf { it in 1..65535 } ?: 8080,
                production = production,
                backendToken = backendToken,
                geminiApiKey = geminiApiKey,
                geminiBaseUrl = read("GEMINI_BASE_URL")?.trim()?.trimEnd('/')
                    ?.takeIf { it.startsWith("https://") }
                    ?: "https://generativelanguage.googleapis.com/v1beta",
                geminiModel = geminiModel,
                allowedOrigins = read("ALLOWED_ORIGINS")
                    ?.split(',')
                    ?.map(String::trim)
                    ?.filter(String::isNotEmpty)
                    .orEmpty(),
            )
        }
    }
}
