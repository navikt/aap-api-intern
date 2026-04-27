package no.nav.aap.api.arena

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.jackson.jackson
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import io.prometheus.metrics.core.metrics.Summary
import java.time.Duration
import kotlin.time.Duration.Companion.seconds
import no.nav.aap.api.ArenaoppslagConfig
import no.nav.aap.api.Metrics.prometheus
import no.nav.aap.api.intern.PerioderResponse
import no.nav.aap.api.util.auth.AzureAdTokenProvider
import no.nav.aap.api.util.circuitBreaker
import no.nav.aap.api.util.findRootCause
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerResponse
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import org.slf4j.LoggerFactory

private val secureLog = LoggerFactory.getLogger("team-logs")
private val log = LoggerFactory.getLogger(ArenaoppslagGateway::class.java)

private const val ARENAOPPSLAG_CLIENT_SECONDS_METRICNAME = "arenaoppslag_client_seconds"
private val clientLatencyStats: Summary = Summary.builder().name(ARENAOPPSLAG_CLIENT_SECONDS_METRICNAME)
    .quantile(0.5, 0.05) // Add 50th percentile (= median) with 5% tolerated error
    .quantile(0.9, 0.01) // Add 90th percentile with 1% tolerated error
    .quantile(0.99, 0.001) // Add 99th percentile with 0.1% tolerated error
    .help("Latency arenaoppslag, in seconds").register()

private val objectMapper =
    jacksonObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).registerModule(JavaTimeModule())


class ArenaoppslagGateway(
    private val arenaoppslagConfig: ArenaoppslagConfig,
    private val slowRequestMillis: Long = 2000,
    private val timeoutMillis: Long = 20_000,
) : IArenaoppslagGateway {
    private val tokenProvider = AzureAdTokenProvider()
    private val circuitBreaker = circuitBreaker("arenaoppslag-circuit-breaker") {
        // Mange kall til arenaoppslag tar gjerne 300-400ms har vi sett av prometheus-metrikker.
        // Legger denne derfor litt over dette
        slowCallDurationThreshold = Duration.ofMillis(slowRequestMillis)
    }

    private val maksimumCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(15))
        .build<String, Maksimum>()

    private val personEksistererCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(15))
        .build<String, PersonEksistererIAAPArena>()

    init {
        CaffeineCacheMetrics.monitor(prometheus, maksimumCache, "arenaoppslag_maksimum_cache")
    }

    override suspend fun hentPerioder(
        callId: String, vedtakRequest: InternVedtakRequest,
    ): PerioderResponse = gjørArenaOppslag<PerioderResponse, InternVedtakRequest>(
        "/intern/perioder", callId, vedtakRequest
    ).getOrThrow()

    override suspend fun hentPerioderInkludert11_17(
        callId: String, req: InternVedtakRequest,
    ): PerioderMed11_17Response = gjørArenaOppslag<PerioderMed11_17Response, InternVedtakRequest>(
        "/intern/perioder/11-17", callId, req
    ).getOrThrow()

    override suspend fun hentSakerByFnr(
        callId: String, req: SakerRequest,
    ): List<SakStatus> = gjørArenaOppslag<List<SakStatus>, SakerRequest>(
        "/intern/saker", callId, req
    ).getOrThrow()

    override suspend fun hentMaksimum(
        callId: String, req: InternVedtakRequest,
    ): Maksimum {
        val key = req.toString()
        return maksimumCache.getIfPresent(key)
            ?: gjørArenaOppslag<Maksimum, InternVedtakRequest>("/intern/maksimum", callId, req)
                .getOrThrow()
                .also { maksimumCache.put(key, it) }
    }

    override suspend fun hentPersonEksistererIAapContext(
        callId: String, req: SakerRequest,
    ): PersonEksistererIAAPArena {
        val key = req.toString()
        return personEksistererCache.getIfPresent(key)
            ?: gjørArenaOppslag<PersonEksistererIAAPArena, SakerRequest>(
                "/api/v1/person/eksisterer", callId, req
            ).getOrThrow()
                .also { personEksistererCache.put(key, it) }
    }

    override suspend fun hentPersonHarSignifikantHistorikk(
        callId: String,
        req: SignifikanteSakerRequest,
    ): SignifikanteSakerResponse =
        gjørArenaOppslag<SignifikanteSakerResponse, SignifikanteSakerRequest>(
            "/api/v1/person/signifikant-historikk", callId, req
        ).getOrThrow()

    private suspend inline fun <reified T, reified V> gjørArenaOppslag(
        endepunkt: String, callId: String, req: V,
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
            requestTimeoutMillis = timeoutMillis
            connectTimeoutMillis = 20.seconds.inWholeMilliseconds
            socketTimeoutMillis = 20.seconds.inWholeMilliseconds
        }

        install(HttpRequestRetry) {
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
