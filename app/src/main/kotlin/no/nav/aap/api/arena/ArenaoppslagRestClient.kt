package no.nav.aap.api.arena

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.prometheus.metrics.core.metrics.Summary
import no.nav.aap.api.ArenaoppslagConfig
import no.nav.aap.api.intern.PerioderResponse
import no.nav.aap.api.util.circuitBreaker
import no.nav.aap.api.util.findRootCause
import no.nav.aap.arenaoppslag.kontrakt.intern.*
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import org.slf4j.LoggerFactory
import java.time.Duration

private val secureLog = LoggerFactory.getLogger("secureLog")
private val log = LoggerFactory.getLogger(ArenaoppslagRestClient::class.java)

private const val ARENAOPPSLAG_CLIENT_SECONDS_METRICNAME = "arenaoppslag_client_seconds"
private val clientLatencyStats: Summary = Summary.builder().name(ARENAOPPSLAG_CLIENT_SECONDS_METRICNAME)
    .quantile(0.5, 0.05) // Add 50th percentile (= median) with 5% tolerated error
    .quantile(0.9, 0.01) // Add 90th percentile with 1% tolerated error
    .quantile(0.99, 0.001) // Add 99th percentile with 0.1% tolerated error
    .help("Latency arenaoppslag, in seconds").register()

private val objectMapper =
    jacksonObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).registerModule(JavaTimeModule())


class ArenaoppslagRestClient(
    private val arenaoppslagConfig: ArenaoppslagConfig,
    azureConfig: no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig,
) : IArenaoppslagRestClient {
    private val tokenProvider = no.nav.aap.api.util.auth.AzureAdTokenProvider(azureConfig)
    private val circuitBreaker = circuitBreaker("arenaoppslag-circuit-breaker") {
        // Mange kall til arenaoppslag tar gjerne 300-400ms har vi sett av prometheus-metrikker. Legger denne derfor litt over dette
        slowCallDurationThreshold = Duration.ofMillis(600)
    }

    override suspend fun hentPerioder(
        callId: String, vedtakRequest: InternVedtakRequest
    ): PerioderResponse = gjørArenaOppslag<PerioderResponse, InternVedtakRequest>(
        "/intern/perioder", callId, vedtakRequest
    ).getOrThrow()

    override suspend fun hentPerioderInkludert11_17(
        callId: String, req: InternVedtakRequest,
    ): PerioderMed11_17Response = gjørArenaOppslag<PerioderMed11_17Response, InternVedtakRequest>(
        "/intern/perioder/11-17", callId, req
    ).getOrThrow()

    override suspend fun hentPersonEksistererIAapContext(
        callId: String, req: SakerRequest,
    ): PersonEksistererIAAPArena = gjørArenaOppslag<PersonEksistererIAAPArena, SakerRequest>(
        "/intern/person/aap/eksisterer", callId, req
    ).getOrThrow()

    override suspend fun hentSakerByFnr(
        callId: String, req: SakerRequest
    ): List<SakStatus> = gjørArenaOppslag<List<SakStatus>, SakerRequest>(
        "/intern/saker", callId, req
    ).getOrThrow()

    override suspend fun hentMaksimum(
        callId: String, req: InternVedtakRequest
    ): Maksimum = gjørArenaOppslag<Maksimum, InternVedtakRequest>(
        "/intern/maksimum", callId, req
    ).getOrThrow()

    private suspend inline fun <reified T, reified V> gjørArenaOppslag(
        endepunkt: String, callId: String, req: V
    ): Result<T> = clientLatencyStats.startTimer().use {
        circuitBreaker.executeSuspendFunction {
            // Vi starter en kjede av kall og prosessering, hvor hvert steg kan feile.
            var fikkToken = false
            var fikkArenaData = false

            val parsedResult = runCatching {
                val token = tokenProvider.getClientCredentialToken(arenaoppslagConfig.scope).also {
                    fikkToken = true
                }

                val arenaResponse = httpClient.post("${arenaoppslagConfig.proxyBaseUrl}$endepunkt") {
                    accept(ContentType.Application.Json)
                    header("Nav-Call-Id", callId)
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(req)
                }.also { it ->
                    if (it.status.isSuccess()) {
                        fikkArenaData = true
                    }
                }

                objectMapper.readValue<T>(arenaResponse.bodyAsText())
            }.onFailure { e ->
                val årsak = e.findRootCause()
                when {
                    !fikkToken -> log.error("Fetch av token for Arena-oppslag feilet: ${årsak.message}", e)
                    !fikkArenaData -> log.error("Fetch av Arena-data feilet for '$endepunkt': ${årsak.message}", e)
                    else -> {
                        log.error("Parsefeil for '$endepunkt'. Se securelog for stacktrace.")
                        secureLog.error("Parsefeil for '$endepunkt': ${årsak.message}", e)
                    }
                }
            }
            parsedResult
        }
    }


    private val httpClient = HttpClient(CIO) {
        expectSuccess = true // Kaster exception for 4xx og 5xx svar

        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 20_000
            socketTimeoutMillis = 20_000
        }

        install(HttpRequestRetry){
            retryOnException(maxRetries = 3) // retry on exception during network send, other than timeout exceptions
            exponentialDelay()
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
