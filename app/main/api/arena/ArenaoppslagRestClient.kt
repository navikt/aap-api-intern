package api.arena

import api.ArenaoppslagConfig
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.prometheus.metrics.core.metrics.Summary
import no.nav.aap.api.intern.PerioderResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import no.nav.aap.ktor.client.auth.azure.AzureAdTokenProvider
import no.nav.aap.ktor.client.auth.azure.AzureConfig
import org.slf4j.LoggerFactory

private const val ARENAOPPSLAG_CLIENT_SECONDS_METRICNAME = "arenaoppslag_client_seconds"
private val sikkerLogg = LoggerFactory.getLogger("secureLog")
private val clientLatencyStats: Summary = Summary.builder()
    .name(ARENAOPPSLAG_CLIENT_SECONDS_METRICNAME)
    .quantile(0.5, 0.05) // Add 50th percentile (= median) with 5% tolerated error
    .quantile(0.9, 0.01) // Add 90th percentile with 1% tolerated error
    .quantile(0.99, 0.001) // Add 99th percentile with 0.1% tolerated error
    .help("Latency arenaoppslag, in seconds")
    .register()

private val objectMapper = jacksonObjectMapper()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .registerModule(JavaTimeModule())

class ArenaoppslagRestClient(
    private val arenaoppslagConfig: ArenaoppslagConfig,
    azureConfig: AzureConfig,
) : IArenaoppslagRestClient {
    private val tokenProvider = AzureAdTokenProvider(azureConfig)

    override suspend fun hentPerioder(callId: String, vedtakRequest: InternVedtakRequest): PerioderResponse =
        clientLatencyStats.startTimer().use {
            httpClient.post("${arenaoppslagConfig.proxyBaseUrl}/intern/perioder") {
                accept(ContentType.Application.Json)
                header("Nav-Call-Id", callId)
                bearerAuth(tokenProvider.getClientCredentialToken(arenaoppslagConfig.scope))
                contentType(ContentType.Application.Json)
                setBody(vedtakRequest)
            }
                .bodyAsText()
                .let(objectMapper::readValue)
        }

    override suspend fun hentPerioderInkludert11_17(
        callId: String,
        vedtakRequest: InternVedtakRequest,
    ): PerioderMed11_17Response =
        clientLatencyStats.startTimer().use {
            httpClient.post("${arenaoppslagConfig.proxyBaseUrl}/intern/perioder/11-17") {
                accept(ContentType.Application.Json)
                header("Nav-Call-Id", callId)
                bearerAuth(tokenProvider.getClientCredentialToken(arenaoppslagConfig.scope))
                contentType(ContentType.Application.Json)
                setBody(vedtakRequest)
            }
                .bodyAsText()
                .let(objectMapper::readValue)
        }

    override suspend fun hentPersonEksistererIAapContext(
        callId: String,
        sakerRequest: SakerRequest,
    ): PersonEksistererIAAPArena =
        clientLatencyStats.startTimer().use {
            httpClient.post("${arenaoppslagConfig.proxyBaseUrl}/intern/person/aap/eksisterer") {
                accept(ContentType.Application.Json)
                header("Nav-Call-Id", callId)
                bearerAuth(tokenProvider.getClientCredentialToken(arenaoppslagConfig.scope))
                contentType(ContentType.Application.Json)
                setBody(sakerRequest)
            }
                .bodyAsText()
                .let(objectMapper::readValue)
        }

    override suspend fun hentSakerByFnr(
        callId: String,
        req: SakerRequest,
    ): List<no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus> =
        clientLatencyStats.startTimer().use {
            httpClient.post("${arenaoppslagConfig.proxyBaseUrl}/intern/saker") {
                accept(ContentType.Application.Json)
                header("Nav-Call-Id", callId)
                bearerAuth(tokenProvider.getClientCredentialToken(arenaoppslagConfig.scope))
                contentType(ContentType.Application.Json)
                setBody(req)
            }
                .bodyAsText()
                .let(objectMapper::readValue)
        }

    override suspend fun hentMaksimum(callId: String, req: InternVedtakRequest): Maksimum {
        return clientLatencyStats.startTimer().use {
            val token = tokenProvider.getClientCredentialToken(arenaoppslagConfig.scope)
            httpClient.post("${arenaoppslagConfig.proxyBaseUrl}/intern/maksimum") {
                accept(ContentType.Application.Json)
                header("Nav-Call-Id", callId)
                bearerAuth(token)
                contentType(ContentType.Application.Json)
                setBody(req)
            }
                .bodyAsText()
                .let(objectMapper::readValue)
        }
    }

    private val httpClient = HttpClient(CIO) {
        install(HttpTimeout)
        install(HttpRequestRetry)
        install(Logging) {
            level = LogLevel.BODY
            logger = object : Logger {
                override fun log(message: String) {
                    sikkerLogg.info("HTTP client log: '$message'.")
                }
            }
        }

        install(ContentNegotiation) {
            jackson {
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                registerModule(JavaTimeModule())
            }
        }
    }
}
