package no.nav.aap.api.arena

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import io.prometheus.metrics.core.metrics.Summary
import no.nav.aap.api.ArenaoppslagConfig
import no.nav.aap.api.Metrics.prometheus
import no.nav.aap.api.intern.PerioderResponse
import no.nav.aap.api.util.auth.AzureAdTokenProvider
import no.nav.aap.api.util.circuitBreaker
import no.nav.aap.api.util.findRootCause
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakMedVedtakResponse as ArenaSakMedVedtakResponseV1
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.time.Duration.Companion.seconds
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerRequest as SakerRequestV1

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

@Suppress("MagicNumber")
class ArenaoppslagGateway(
    private val arenaoppslagConfig: ArenaoppslagConfig,
    private val slowRequestMillis: Long = 2000,
    private val timeoutMillis: Long = 20_000,
    private val cacheName: String = "arenaoppslag_maksimum_cache",
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
        .recordStats()
        .build<String, Maksimum>()

    private val personEksistererCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(15))
        .recordStats()
        .build<String, PersonEksistererIAAPArena>()

    init {
        CaffeineCacheMetrics.monitor(prometheus, maksimumCache, cacheName)
    }

    override suspend fun hentPerioder(
        callId: String, vedtakRequest: InternVedtakRequest,
    ): PerioderResponse = gjørArenaPostOppslag<PerioderResponse, InternVedtakRequest>(
        "/intern/perioder", callId, vedtakRequest
    ).getOrThrow()

    override suspend fun hentPerioderInkludert11_17(
        callId: String, req: InternVedtakRequest,
    ): PerioderMed11_17Response = gjørArenaPostOppslag<PerioderMed11_17Response, InternVedtakRequest>(
        "/intern/perioder/11-17", callId, req
    ).getOrThrow()

    override suspend fun hentSakerByFnr(
        callId: String, req: SakerRequest,
    ): List<SakStatus> = gjørArenaPostOppslag<List<SakStatus>, SakerRequest>(
        "/intern/saker", callId, req
    ).getOrThrow()

    override suspend fun hentSakerForPerson(
        callId: String, req: SakerRequestV1,
    ): SakerResponse = gjørArenaPostOppslag<SakerResponse, SakerRequestV1>(
        "/api/v1/person/saker", callId, req, tillattMed404 = true
    ).recover { throwable ->
        if (responseStatus(throwable) == HttpStatusCode.NotFound) {
            secureLog.warn("Personen ble ikke funnet i Arena [personidentifikator=${req.personidentifikator}]")
            // Personen ble ikke funnet i Arena – returner tom liste med saker
            SakerResponse(emptyList())
        } else {
            throw throwable
        }
    }.getOrThrow()

    override suspend fun hentMaksimum(
        callId: String, req: InternVedtakRequest,
    ): Maksimum {
        val key = req.toString()
        return maksimumCache.getIfPresent(key)
            ?: gjørArenaPostOppslag<Maksimum, InternVedtakRequest>("/intern/maksimum", callId, req)
                .getOrThrow()
                .also { maksimumCache.put(key, it) }
    }

    override suspend fun hentPersonEksistererIAapContext(
        callId: String, req: SakerRequest,
    ): PersonEksistererIAAPArena {
        val key = req.toString()
        return personEksistererCache.getIfPresent(key)
            ?: gjørArenaPostOppslag<PersonEksistererIAAPArena, SakerRequest>(
                "/api/v1/person/eksisterer", callId, req
            ).getOrThrow()
                .also { personEksistererCache.put(key, it) }
    }

    override suspend fun hentArenaSakMedVedtak(
        callId: String, sakId: String
    ): ArenaSakMedVedtakResponseV1? =
        gjørArenaGetOppslag<ArenaSakMedVedtakResponseV1>("/api/v1/sak/$sakId", callId, tillattMed404 = true)
            .recover { throwable ->
                if (responseStatus(throwable) == HttpStatusCode.NotFound) null
                else throw throwable
            }
            .getOrThrow()

    private suspend inline fun <reified T, reified V> gjørArenaPostOppslag(
        endepunkt: String, callId: String, req: V, tillattMed404: Boolean = false
    ): Result<T> = clientLatencyStats.startTimer().use {
        circuitBreaker.executeSuspendFunction {
            var fikkToken = false
            var fikkArenaData = false

            runCatching {
                val token = tokenProvider.getClientCredentialToken(arenaoppslagConfig.scope)
                    .also { fikkToken = true }

                val response = httpClient.post("${arenaoppslagConfig.proxyBaseUrl}$endepunkt") {
                    accept(ContentType.Application.Json)
                    header("Nav-Call-Id", callId)
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(req)
                }.also { fikkArenaData = true }

                objectMapper.readValue<T>(response.bodyAsText())
            }.onFailure { e -> logArenaFeil(e, endepunkt, tillattMed404, fikkToken, fikkArenaData) }
        }
    }

    private suspend inline fun <reified T> gjørArenaGetOppslag(
        endepunkt: String, callId: String, tillattMed404: Boolean = false
    ): Result<T> = clientLatencyStats.startTimer().use {
        circuitBreaker.executeSuspendFunction {
            var fikkToken = false
            var fikkArenaData = false

            runCatching {
                val token = tokenProvider.getClientCredentialToken(arenaoppslagConfig.scope)
                    .also { fikkToken = true }

                val response = httpClient.get("${arenaoppslagConfig.proxyBaseUrl}$endepunkt") {
                    accept(ContentType.Application.Json)
                    header("Nav-Call-Id", callId)
                    bearerAuth(token)
                }.also { fikkArenaData = true }

                objectMapper.readValue<T>(response.bodyAsText())
            }.onFailure { e -> logArenaFeil(e, endepunkt, tillattMed404, fikkToken, fikkArenaData) }
        }
    }

    private fun logArenaFeil(
        e: Throwable, endepunkt: String,
        tillattMed404: Boolean, fikkToken: Boolean, fikkArenaData: Boolean
    ) {
        if (tillattMed404 && responseStatus(e) == HttpStatusCode.NotFound) return

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

    private fun responseStatus(throwable: Throwable): HttpStatusCode? =
        generateSequence(throwable) { it.cause }
            .filterIsInstance<ClientRequestException>()
            .firstOrNull()?.response?.status

    private val httpClient = HttpClient(CIO) {
        expectSuccess = true // Kaster exception for 4xx og 5xx svar

        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMillis
            connectTimeoutMillis = 20.seconds.inWholeMilliseconds
            socketTimeoutMillis = 20.seconds.inWholeMilliseconds
        }

        install(HttpRequestRetry) {
            // Retry på transiente nettverksfeil og 5xx – ikke på 4xx-klientfeil og ikke på timeouts.
            // Timeouts retries ikke fordi vi heller vil feile raskt enn å akkumulere ventetid.
            retryOnExceptionIf(maxRetries = 3) { _, cause ->
                responseStatus(cause) != HttpStatusCode.NotFound &&
                        cause !is HttpRequestTimeoutException &&
                        cause !is ConnectTimeoutException &&
                        cause !is SocketTimeoutException
            }
            retryOnServerErrors(maxRetries = 3) // 5xx
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
