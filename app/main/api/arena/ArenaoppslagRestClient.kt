package api.arena

import api.ArenaoppslagConfig
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.prometheus.metrics.core.metrics.Summary
import no.nav.aap.api.intern.PerioderResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum

private const val ARENAOPPSLAG_CLIENT_SECONDS_METRICNAME = "arenaoppslag_client_seconds"
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
    azureConfig: no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig,
) : IArenaoppslagRestClient {
    private val tokenProvider = api.util.auth.AzureAdTokenProvider(azureConfig)

    override suspend fun hentPerioder(
        callId: String,
        vedtakRequest: InternVedtakRequest
    ): PerioderResponse =
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

        install(ContentNegotiation) {
            jackson {
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                registerModule(JavaTimeModule())
            }
        }
    }
}
