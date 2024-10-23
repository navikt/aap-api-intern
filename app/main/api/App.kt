package api

import api.arena.ArenaoppslagRestClient
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger("App")
private val sikkerLogg = LoggerFactory.getLogger("secureLog")

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e -> logger.error("Uhåndtert feil", e) }
    embeddedServer(Netty, port = 8080, module = Application::api).start(wait = true)
}

fun PrometheusMeterRegistry.httpCallCounter(path: String): Counter = this.counter(
    "http_call",
    listOf(Tag.of("path", path))
)

fun Application.api() {
    val config = Config()
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val arenaRestClient = ArenaoppslagRestClient(config.arenaoppslag, config.azure)

    install(CallLogging) {
        level = Level.INFO
        format { call ->
            val status = call.response.status()
            val errorBody =
                if (status?.value != null && status.value > 499) ", ErrorBody: ${call.response}" else ""
            val httpMethod = call.request.httpMethod.value
            val userAgent = call.request.headers["User-Agent"]
            val callId =
                call.request.header("x-callid") ?: call.request.header("nav-callId") ?: "ukjent"
            val path = call.request.path()
            "Status: $status $errorBody, HTTP method: $httpMethod, User agent: $userAgent, Call id: $callId, Path: $path"
        }
        filter { call -> call.request.path().startsWith("/actuator").not() }
    }

    install(CallId) {
        retrieveFromHeader(HttpHeaders.XCorrelationId)
        generate { UUID.randomUUID().toString() }
    }

    install(MicrometerMetrics) {
        registry = prometheus
        meterBinders += LogbackMetrics()
    }

    install(ContentNegotiation) {
        register(
            ContentType.Application.Json,
            JacksonConverter(objectMapper = DefaultJsonMapper.objectMapper(), true)
        )
    }

    val jwkProvider = JwkProviderBuilder(URI(config.azure.jwksUri).toURL())
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()
    authentication {
        jwt {
            realm = "intern api"
            verifier(jwkProvider, config.azure.issuer)
            challenge { a, b ->
                logger.warn("Unauthorized: $a - $b")
                call.respond(HttpStatusCode.Unauthorized)
            }
            validate { credential ->
                sikkerLogg.info("Sjekk: ${config.azure.clientId} og ${credential.payload.expiresAt} og ${credential.payload.notBefore} og ${credential.payload.audience}")
                if (credential.payload.audience.contains(config.azure.clientId)) JWTPrincipal(
                    credential.payload
                ) else null
            }
        }
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    routing {
        actuator(prometheus)
        api(arenaRestClient, prometheus)
        swaggerUI(path = "swagger", swaggerFile = "openapi.yaml")
    }
}
