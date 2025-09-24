package api

import api.arena.IArenaoppslagRestClient
import api.pdl.IPdlClient
import api.postgres.*
import api.util.fraKontrakt
import api.util.fraKontraktUtenUtbetaling
import api.util.perioderMedAAp
import com.papsign.ktor.openapigen.APITag
import com.papsign.ktor.openapigen.annotations.parameters.HeaderParam
import com.papsign.ktor.openapigen.annotations.properties.description.Description
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.api.intern.*
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.server.auth.audience
import no.nav.aap.komponenter.server.auth.token
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

private val logger = LoggerFactory.getLogger("App")

data class CallIdHeader(
    @param:HeaderParam("callId") val `Nav-CallId`: String? = null,
    @param:HeaderParam("correlation id") val `X-Correlation-Id`: String? = null,
) {
    fun callId(): String? {
        val callId = listOfNotNull(`Nav-CallId`, `X-Correlation-Id`).firstOrNull()

        return callId
    }
}

enum class Tag(override val description: String) : APITag {
    Perioder("For å hente perioder med AAP"),
    Saker("For å hente AAP-saker"),
    Meldekort("For å hente AAP-meldekort"),
    Maksimum("For å hente maksimumsløsning")
}

data class SakerRequest(
    @param:Description("Liste med personidentifikatorer. Må svare til samme person.")
    val personidentifikatorer: List<String>
)

fun NormalOpenAPIRoute.api(
    dataSource: DataSource,
    arena: IArenaoppslagRestClient,
    prometheus: PrometheusMeterRegistry,
    pdlClient: IPdlClient,
    nå: LocalDate = LocalDate.now(),
) {
    tag(Tag.Perioder) {
        route("/perioder") {
            post<CallIdHeader, PerioderResponse, InternVedtakRequest>(
                info(description = "Henter perioder med vedtak for en person innen gitte datointervall.")
            ) { callIdHeader, requestBody ->
                logger.info("Henter perioder")
                val azpName = azpName()
                prometheus.httpCallCounter(
                    "/perioder",
                    pipeline.call.audience(),
                    azpName ?: ""
                ).increment()
                val callId = callIdHeader.callId() ?: UUID.randomUUID().toString().also {
                    logger.info("CallID ble ikke gitt på kall mot: /perioder")
                }

                sjekkTilgangTilPerson(listOf(requestBody.personidentifikator))

                val kelvinPerioder = dataSource.transaction { connection ->
                    val behandlingsRepository = BehandlingsRepository(connection)
                    val vedtaksdata =
                        behandlingsRepository.hentVedtaksData(
                            requestBody.personidentifikator,
                            Periode(requestBody.fraOgMedDato, requestBody.tilOgMedDato)
                        )
                    perioderMedAAp(
                        vedtaksdata
                    )
                }

                val arenaPerioder = arena.hentPerioder(
                    callId,
                    requestBody
                ).perioder

                prometheus.tellKildesystem(kelvinPerioder, arenaPerioder, "/perioder")

                respond(
                    PerioderResponse(
                        arenaPerioder + kelvinPerioder
                    )
                )
            }

            route("/aktivitetfase").post<CallIdHeader, PerioderInkludert11_17Response, InternVedtakRequest>(
                info(description = "Henter perioder med vedtak fra Arena (aktivitetsfase) for en person innen gitte datointervall.")
            ) { callIdHeader, requestBody ->
                prometheus.httpCallCounter(
                    "/perioder/aktivitetfase",
                    pipeline.call.audience(),
                    azpName() ?: ""
                )
                    .increment()
                val callId = callIdHeader.callId() ?: UUID.randomUUID().toString().also {
                    logger.info("CallID ble ikke gitt på kall mot: /perioder/aktivitetfase")
                }

                sjekkTilgangTilPerson(listOf(requestBody.personidentifikator))

                val arenaSvar = arena.hentPerioderInkludert11_17(callId, requestBody)

                prometheus.tellKildesystem(
                    null,
                    arenaSvar.perioder,
                    "/perioder/aktivitetfase"
                )

                respond(
                    PerioderInkludert11_17Response(
                        perioder = arenaSvar.perioder.map {
                            PeriodeInkludert11_17(
                                periode = it.periode.let {
                                    Periode(
                                        it.fraOgMedDato,
                                        it.tilOgMedDato
                                    )
                                },
                                aktivitetsfaseKode = it.aktivitetsfaseKode,
                                aktivitetsfaseNavn = it.aktivitetsfaseNavn,
                            )
                        }
                    )
                )
            }

            // FIXME bør ha et mer spesifikt navn enn meldekort, f.eks. meldekortperioder
            route("/meldekort").post<CallIdHeader, List<Periode>, InternVedtakRequest>(
                info(description = "Henter meldekort perioder for en person innen gitte datointerval")
            ) { _, requestBody ->

                sjekkTilgangTilPerson(listOf(requestBody.personidentifikator))

                val perioder = dataSource.transaction { connection ->
                    val meldekortPerioderRepository = MeldekortPerioderRepository(connection)
                    meldekortPerioderRepository.hentMeldekortPerioder(requestBody.personidentifikator)
                }.filter { it.tom > requestBody.fraOgMedDato && it.fom < requestBody.tilOgMedDato }

                prometheus.tellKildesystem(perioder, null, "/perioder/meldekort")

                respond(perioder, HttpStatusCode.OK)
            }
        }
    }

    tag(Tag.Meldekort) {

        route("/kelvin/meldekort-detaljer").post<CallIdHeader, MeldekortDetaljerResponse, MeldekortDetaljerRequest>(
            info(description = "Henter detaljerte meldekort for en gitt person og evt. begrenset til en gitt periode")
        ) { _, requestBody ->

            val personIdentifikator = requestBody.personidentifikator
            sjekkTilgangTilPerson(listOf(personIdentifikator))

            val meldekortListe = dataSource.transaction { connection ->
                val meldekortService = MeldekortService(connection,pdlClient)
                meldekortService.hentAlle(personIdentifikator, requestBody.fraOgMedDato, requestBody.tilOgMedDato)
                    .map { (meldekort, vedtak) ->
                        meldekort.tilKontrakt(vedtak)
                    }
            }

            prometheus.tellKelvinKall(pipeline.call.request)

            if (meldekortListe.isEmpty()) {
                logger.info("Fant ingen meldekort for person $personIdentifikator i den angitte perioden")
            }

            val responseBody = MeldekortDetaljerResponse(personIdentifikator, meldekortListe)
            respond(responseBody, HttpStatusCode.OK)
        }

    }

    tag(Tag.Saker) {
        // TODO: Flytt logikk til en egen service
        route("/sakerByFnr").post<CallIdHeader, List<SakStatus>, api.SakerRequest>(
            info(description = "Henter saker for en person")
        ) { callIdHeader, requestBody ->
            prometheus.httpCallCounter(
                "/sakerByFnr",
                pipeline.call.audience(),
                azpName() ?: ""
            ).increment()

            val callId = callIdHeader.callId() ?: UUID.randomUUID().toString().also {
                logger.info("CallID ble ikke gitt på kall mot: /sakerByFnr")
            }

            sjekkTilgangTilPerson(requestBody.personidentifikatorer)

            val personIdenter = hentAllePersonidenter(requestBody.personidentifikatorer, pdlClient)
            val kelvinSaker: List<SakStatus> =
                dataSource.transaction { connection ->
                    val sakStatusRepository = SakStatusRepository(connection)
                    personIdenter.flatMap {
                        sakStatusRepository.hentSakStatus(it)
                    }
                }
            val arenaSaker: List<SakStatus> =
                arena.hentSakerByFnr(callId, SakerRequest(personIdenter)).map {
                    arenaSakStatusTilDomene(it)
                }

            prometheus.tellKildesystem(kelvinSaker, arenaSaker, "/sakerByFnr")

            respond(arenaSaker + kelvinSaker)
        }

        route("/arena/person/aap/eksisterer") {
            post<CallIdHeader, PersonEksistererIAAPArena, api.SakerRequest>(
                info(description = "Sjekker om en person eksisterer i AAP-arena")
            ) { callIdHeader, requestBody ->
                logger.info("Sjekker om person eksisterer i aap-arena")
                prometheus.httpCallCounter(
                    "/arena/person/aap/eksisterer",
                    pipeline.call.audience(),
                    azpName() ?: ""
                ).increment()
                val callId = callIdHeader.callId() ?: UUID.randomUUID().toString().also {
                    logger.info("CallID ble ikke gitt på kall mot: /person/aap/eksisterer")
                }

                pipeline.call.response.headers.append(
                    HttpHeaders.ContentType,
                    ContentType.Application.Json.withCharset(Charsets.UTF_8).toString()
                )
                respond(
                    PersonEksistererIAAPArena(
                        arena.hentPersonEksistererIAapContext(
                            callId,
                            SakerRequest(requestBody.personidentifikatorer)
                        ).eksisterer
                    )
                )
            }
        }

        route("/kelvin/sakerByFnr").post<CallIdHeader, List<SakStatus>, api.SakerRequest>(
            info(description = "Henter saker for en person")
        ) { _, requestBody ->
            logger.info("Henter saker for en person fra Kelvin.")
            prometheus.httpCallCounter(
                "/kelvin/sakerByFnr",
                pipeline.call.audience(),
                azpName() ?: ""
            ).increment()

            sjekkTilgangTilPerson(requestBody.personidentifikatorer)

            val personIdenter = hentAllePersonidenter(requestBody.personidentifikatorer, pdlClient)
            val kelvinSaker: List<SakStatus> =
                dataSource.transaction { connection ->
                    val sakStatusRepository = SakStatusRepository(connection)
                    personIdenter.flatMap {
                        sakStatusRepository.hentSakStatus(it)
                    }
                }
            prometheus.tellKildesystem(kelvinSaker, null, "/kelvin/sakerByFnr")
            respond(kelvinSaker)
        }
    }

    tag(Tag.Maksimum) {
        route("/maksimumUtenUtbetaling") {
            post<CallIdHeader, Medium, InternVedtakRequestApiIntern>(
                info(
                    description = """
                    Henter maksimumsløsning uten utbetalinger for en person innen gitte datointervall.
                    dagsatsEtterUføreReduksjon er kun tilgjengelig fra Kelvin.
                    """.trimIndent()
                )
            ) { callIdHeader, requestBody ->
                prometheus.httpCallCounter(
                    "/maksimumUtenUtbetaling",
                    pipeline.call.audience(),
                    azpName() ?: ""
                ).increment()
                val callId = callIdHeader.callId() ?: UUID.randomUUID().toString().also {
                    logger.info("CallID ble ikke gitt på kall mot: /maksimumUtenUtbetaling")
                }
                val body = requestBody.tilKontrakt()
                sjekkTilgangTilPerson(listOf(requestBody.personidentifikator))

                val kelvinSaker: List<VedtakUtenUtbetaling> = dataSource.transaction { connection ->
                    val behandlingsRepository = BehandlingsRepository(connection)
                    VedtakService(behandlingsRepository, nå = nå).hentMediumFraKelvin(
                        body.personidentifikator,
                        Periode(body.fraOgMedDato, body.tilOgMedDato)
                    ).vedtak
                }
                pipeline.call.response.headers.append(
                    HttpHeaders.ContentType,
                    ContentType.Application.Json.withCharset(Charsets.UTF_8).toString()
                )

                val arenaRespons = arena.hentMaksimum(
                    callId,
                    body
                ).vedtak.map { it.fraKontraktUtenUtbetaling() }

                prometheus.tellKildesystem(kelvinSaker, arenaRespons, "/maksimumUtenUtbetaling")

                respond(Medium(arenaRespons + kelvinSaker))
            }
        }
        route("/maksimum") {
            post<CallIdHeader, Maksimum, InternVedtakRequestApiIntern>(
                info(
                    description = """
                    Henter maksimumsløsning for en person innen gitte datointervall. Behandlinger før 18/8 inneholder ikke beregningsgrunnlag.
                    dagsatsEtterUføreReduksjon er kun tilgjengelig fra Kelvin""".trimIndent()
                )
            ) { callIdHeader, requestBody ->
                logger.info("Henter maksimum")
                prometheus.httpCallCounter(
                    "/maksimum",
                    pipeline.call.audience(),
                    azpName() ?: ""
                ).increment()
                val callId = callIdHeader.callId() ?: UUID.randomUUID().toString().also {
                    logger.info("CallID ble ikke gitt på kall mot: /maksimum")
                }
                val body = requestBody.tilKontrakt()
                sjekkTilgangTilPerson(listOf(requestBody.personidentifikator))

                val kelvinSaker: List<Vedtak> = dataSource.transaction { connection ->
                    val behandlingsRepository = BehandlingsRepository(connection)
                    VedtakService(behandlingsRepository, nå = nå).hentMaksimum(
                        requestBody.personidentifikator,
                        Periode(body.fraOgMedDato, body.tilOgMedDato),
                    ).vedtak
                }
                val arenaVedtak = arena.hentMaksimum(callId, body).fraKontrakt().vedtak

                prometheus.tellKildesystem(kelvinSaker, arenaVedtak, "/maksimum")

                pipeline.call.response.headers.append(
                    HttpHeaders.ContentType,
                    ContentType.Application.Json.withCharset(Charsets.UTF_8).toString()
                )

                respond(
                    Maksimum(
                        arenaVedtak + kelvinSaker
                    )
                )
            }
        }
        route("/kelvin/") {
            route("maksimumUtenUtbetaling").post<CallIdHeader, Medium, InternVedtakRequest>(
                info(description = "Henter maksimumsløsning uten utbetalinger fra kelvin for en person innen gitte datointerval. Behandlinger før 18/8 inneholder ikke beregningsgrunnlag.")
            ) { _, requestBody ->
                logger.info("Henter maksimum uten utbetalinger fra kelvin")
                prometheus.httpCallCounter(
                    "/kelvin/maksimumUtenUtbetaling",
                    pipeline.call.audience(),
                    azpName() ?: ""
                ).increment()

                sjekkTilgangTilPerson(listOf(requestBody.personidentifikator))

                val kelvinSaker: List<VedtakUtenUtbetaling> = dataSource.transaction { connection ->
                    val behandlingsRepository = BehandlingsRepository(connection)
                    VedtakService(behandlingsRepository, nå = nå).hentMediumFraKelvin(
                        requestBody.personidentifikator,
                        Periode(requestBody.fraOgMedDato, requestBody.tilOgMedDato),
                    ).vedtak
                }
                pipeline.call.response.headers.append(
                    HttpHeaders.ContentType,
                    ContentType.Application.Json.withCharset(Charsets.UTF_8).toString()
                )

                prometheus.tellKildesystem(kelvinSaker, null, "/kelvin/maksimumUtenUtbetaling")

                respond(Medium(kelvinSaker))
            }

            route("behandling").post<CallIdHeader, List<DatadelingDTO>, InternVedtakRequest>(
                info(
                    description = "Henter ut behandlingsdata for en person innen gitte datointerval uten behandling av datasett",
                    deprecated = true
                )
            ) { _, requestBody ->
                prometheus.httpCallCounter(
                    "/kelvin/behandling",
                    pipeline.call.audience(),
                    azpName() ?: ""
                ).increment()

                sjekkTilgangTilPerson(listOf(requestBody.personidentifikator))

                val kelvinSaker = dataSource.transaction { connection ->
                    val behandlingsRepository = BehandlingsRepository(connection)
                    behandlingsRepository.hentVedtaksData(
                        requestBody.personidentifikator,
                        Periode(requestBody.fraOgMedDato, requestBody.tilOgMedDato)
                    )
                }

                prometheus.tellKildesystem(kelvinSaker, null, "/kelvin/behandling")

                respond(kelvinSaker)
            }
        }
    }
}

private fun PrometheusMeterRegistry.tellKelvinKall(request: ApplicationRequest) {
    this.kildesystemTeller("kelvin", request.path()).increment()
}

private fun PrometheusMeterRegistry.tellKildesystem(
    kelvinData: List<*>?,
    arenaData: List<*>?,
    path: String
) {
    if (!kelvinData.isNullOrEmpty()) {
        this.kildesystemTeller("kelvin", path).increment()
    }

    if (!arenaData.isNullOrEmpty()) {
        this.kildesystemTeller("arena", path).increment()
    }

    if (arenaData?.isNotEmpty() == true && kelvinData?.isNotEmpty() == true) {
        logger.error("Fant data på person i både Kelvin og Arena.")
    }
}

private suspend inline fun <reified E : Any> OpenAPIPipelineResponseContext<E>.sjekkTilgangTilPerson(
    identifikatorer: List<String>
) {
    if (!harTilgangTilPerson(identifikatorer.first(), token())) {
        respondWithStatus(HttpStatusCode.Forbidden)
    }
}

private fun harTilgangTilPerson(personIdent: String, token: OidcToken): Boolean {
    if (!token.isClientCredentials()) {
        val tilgang = TilgangGateway.harTilgangTilPerson(personIdent, token)
        if (!tilgang) {
            logger.warn("Tilgang avslått på kall med obo-token mot endepunkt")
            return false
        }
    }
    return true
}

private fun arenaSakStatusTilDomene(it: no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus) =
    SakStatus(
        sakId = it.sakId,
        statusKode = when (it.statusKode) {
            no.nav.aap.arenaoppslag.kontrakt.intern.Status.AVSLU -> no.nav.aap.api.intern.Status.AVSLU
            no.nav.aap.arenaoppslag.kontrakt.intern.Status.FORDE -> no.nav.aap.api.intern.Status.FORDE
            no.nav.aap.arenaoppslag.kontrakt.intern.Status.GODKJ -> no.nav.aap.api.intern.Status.GODKJ
            no.nav.aap.arenaoppslag.kontrakt.intern.Status.INNST -> no.nav.aap.api.intern.Status.INNST
            no.nav.aap.arenaoppslag.kontrakt.intern.Status.IVERK -> no.nav.aap.api.intern.Status.IVERK
            no.nav.aap.arenaoppslag.kontrakt.intern.Status.KONT -> no.nav.aap.api.intern.Status.KONT
            no.nav.aap.arenaoppslag.kontrakt.intern.Status.MOTAT -> no.nav.aap.api.intern.Status.MOTAT
            no.nav.aap.arenaoppslag.kontrakt.intern.Status.OPPRE -> no.nav.aap.api.intern.Status.OPPRE
            no.nav.aap.arenaoppslag.kontrakt.intern.Status.REGIS -> no.nav.aap.api.intern.Status.REGIS
            no.nav.aap.arenaoppslag.kontrakt.intern.Status.UKJENT -> no.nav.aap.api.intern.Status.UKJENT
            no.nav.aap.arenaoppslag.kontrakt.intern.Status.OPPRETTET -> no.nav.aap.api.intern.Status.OPPRETTET
            no.nav.aap.arenaoppslag.kontrakt.intern.Status.UTREDES -> no.nav.aap.api.intern.Status.UTREDES
            no.nav.aap.arenaoppslag.kontrakt.intern.Status.LØPENDE -> no.nav.aap.api.intern.Status.LØPENDE
            no.nav.aap.arenaoppslag.kontrakt.intern.Status.AVSLUTTET -> no.nav.aap.api.intern.Status.AVSLUTTET
        },
        periode = Periode(
            it.periode.fraOgMedDato,
            it.periode.tilOgMedDato
        ),
        kilde = when (it.kilde) {
            no.nav.aap.arenaoppslag.kontrakt.intern.Kilde.ARENA -> Kilde.ARENA
            no.nav.aap.arenaoppslag.kontrakt.intern.Kilde.KELVIN -> Kilde.ARENA
        }
    )

private fun hentAllePersonidenter(
    identerFraRequest: List<String>,
    pdlClient: IPdlClient
): List<String> {
    // Arena har testbrukere som ikke ligger i PDL
    if (Miljø.erDev()) {
        return identerFraRequest
    }

    val identerFraPdl =
        pdlClient.hentAlleIdenterForPerson(identerFraRequest.first()).map { pdlIdent ->
            pdlIdent.ident
        }
    require(identerFraRequest.all { requestIdent -> requestIdent in identerFraPdl }) {
        "Liste med personidentifikatorer i request inneholder identer for mer enn én person"
    }
    return identerFraPdl
}

private fun OpenAPIPipelineResponseContext<*>.azpName(): String? =
    pipeline.call.principal<JWTPrincipal>()?.let {
        it.payload.claims["azp_name"]?.asString()
    }

fun Routing.actuator(prometheus: PrometheusMeterRegistry) {
    route("/actuator") {
        get("/metrics") {
            call.respondText(prometheus.scrape())
        }
        get("/live") {
            call.respond(HttpStatusCode.OK, "api")
        }
        get("/ready") {
            call.respond(HttpStatusCode.OK, "api")
        }
    }
}


fun utledVedtakStatus(
    behandlingStatus: KelvinBehandlingStatus,
    sakStatus: KelvinSakStatus,
    periode: Periode,
    nå: LocalDate = LocalDate.now()
): String =
    if (
        behandlingStatus == KelvinBehandlingStatus.IVERKSETTES ||
        periode.tom.isAfter(nå) ||
        sakStatus != KelvinSakStatus.AVSLUTTET
    ) {
        Status.LØPENDE.toString()
    } else if (behandlingStatus == KelvinBehandlingStatus.AVSLUTTET) {
        Status.AVSLUTTET.toString()
    } else {
        Status.UTREDES.toString()
    }

data class InternVedtakRequestApiIntern(
    val personidentifikator: String,
    val fraOgMedDato: LocalDate? = LocalDate.of(1, 1, 1),
    val tilOgMedDato: LocalDate? = LocalDate.of(9999, 12, 31)
) {
    fun tilKontrakt(): InternVedtakRequest {
        return InternVedtakRequest(
            personidentifikator = personidentifikator,
            fraOgMedDato = fraOgMedDato ?: LocalDate.of(1, 1, 1),
            tilOgMedDato = tilOgMedDato ?: LocalDate.of(9999, 12, 31)
        )
    }
}