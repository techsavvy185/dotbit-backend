package com.tsapps.dotbit.backend

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import java.net.URI

fun main() {
    val config = BackendConfig.fromEnvironment()
    embeddedServer(Netty, host = "0.0.0.0", port = config.port) {
        module(config)
    }.start(wait = true)
}

fun Application.module(
    config: BackendConfig = BackendConfig.fromEnvironment(),
    providerOverride: CorrectionProvider? = null,
) {
    val jsonCodec = Json {
        ignoreUnknownKeys = false
        explicitNulls = false
        encodeDefaults = true
    }
    val policy = CorrectionPolicy()
    val provider = providerOverride ?: config.geminiApiKey?.let {
        GeminiCorrectionProvider(it, config.geminiBaseUrl, config.geminiModel, jsonCodec)
    } ?: DeterministicCorrectionProvider(policy)

    install(ContentNegotiation) { json(jsonCodec) }
    install(CallLogging) { level = Level.INFO }
    install(StatusPages) {
        exception<RequestValidationException> { call, error ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_request", error.message.orEmpty()))
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_json", "Request body must match the correction schema."))
        }
        exception<ProviderPolicyException> { call, error ->
            call.respond(HttpStatusCode.BadGateway, ErrorResponse("unsafe_provider_response", error.message.orEmpty()))
        }
        exception<ProviderUnavailableException> { call, error ->
            call.respond(HttpStatusCode.ServiceUnavailable, ErrorResponse("provider_unavailable", error.message.orEmpty()))
        }
        exception<Throwable> { call, error ->
            this@module.environment.log.error("Unhandled correction request failure", error)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("internal_error", "Request failed."))
        }
    }
    if (config.allowedOrigins.isNotEmpty()) {
        install(CORS) {
            allowMethod(HttpMethod.Post)
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.ContentType)
            config.allowedOrigins.forEach { origin ->
                val uri = URI(origin)
                allowHost(
                    host = uri.authority,
                    schemes = listOfNotNull(uri.scheme),
                )
            }
        }
    }

    routing {
        get("/health") {
            call.respond(
                HealthResponse(
                    status = "ok",
                    correctionProvider = provider.name,
                    correctionModel = provider.modelName,
                ),
            )
        }
        post("/v1/corrections") {
            config.backendToken?.let { expected ->
                val supplied = call.request.headers[HttpHeaders.Authorization]
                    ?.removePrefix("Bearer ")
                    ?.trim()
                if (supplied != expected) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized", "Invalid backend token."))
                    return@post
                }
            }
            val request = call.receive<CorrectionRequest>()
            policy.validateRequest(request)
            val result = if (request.uncertainSpans.isEmpty()) {
                policy.deterministicResult(request)
            } else {
                provider.suggest(request)
            }
            call.respond(policy.validateResult(request, result))
        }
    }
}
