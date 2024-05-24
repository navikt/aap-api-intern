package api

import api.arena.ArenaoppslagRestClient
import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger("App")
private val sikkerLogg = LoggerFactory.getLogger("secureLog")

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e -> logger.error("Uhåndtert feil", e) }
    embeddedServer(Netty, port = 8080, module = Application::api).start(wait = true)
}

fun Application.api() {
    val config = Config()
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val arenaRestClient = ArenaoppslagRestClient(config.arenaoppslag, config.azure)

    install(CallLogging) {
        level = Level.INFO
        format { call ->
            val status = call.response.status()
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            val callId = call.request.header("x-callid") ?: call.request.header("nav-callId") ?: "ukjent"
            "Status: $status, HTTP method: $httpMethod, User agent: $userAgent, callId: $callId"
        }
        filter { call -> call.request.path().startsWith("/actuator").not() }
    }

    install(MicrometerMetrics) {
        registry = prometheus
    }

    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    val jwkProvider = JwkProviderBuilder(config.azure.jwksUri)
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()
    authentication {
        jwt {
            realm = "intern api"
            verifier(jwkProvider, config.azure.issuer)
            challenge { _, _ ->
                logger.warn("Unauthorized call")
                call.respond(HttpStatusCode.Unauthorized)
            }
            validate { credential ->
                sikkerLogg.info("Sjekk: ${config.azure.clientId} og ${credential.payload.expiresAt} og ${credential.payload.notBefore} og ${credential.payload.audience}")
                if (credential.payload.audience.contains(config.azure.clientId)) JWTPrincipal(credential.payload) else null
            }
        }
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    routing {
        actuator(prometheus)
        api(arenaRestClient)
        swaggerUI(path = "swagger", swaggerFile = "openapi.yaml")
    }
}
