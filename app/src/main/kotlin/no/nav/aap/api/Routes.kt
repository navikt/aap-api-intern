package no.nav.aap.api

import com.papsign.ktor.openapigen.APITag
import com.papsign.ktor.openapigen.annotations.parameters.HeaderParam
import com.papsign.ktor.openapigen.annotations.properties.description.Description
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import no.nav.aap.api.arena.IArenaoppslagRestClient
import no.nav.aap.api.intern.*
import no.nav.aap.api.pdl.IPdlClient
import no.nav.aap.api.postgres.*
import no.nav.aap.api.util.fraKontrakt
import no.nav.aap.api.util.fraKontraktUtenUtbetaling
import no.nav.aap.api.util.perioderMedAAp
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.server.auth.token
import no.nav.aap.komponenter.type.Periode
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest as ArenaSakerRequest

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
    Maksimum("For å hente maksimumsløsning"),
    DSOP("For DSOP-relaterte endepunkter"),
}

data class SakerRequest(
    @param:Description("Liste med personidentifikatorer. Må svare til samme person.")
    val personidentifikatorer: List<String>,
)

private fun receiveCall(
    endpoint: String,
    callIdHeader: CallIdHeader,
    pipeline: RoutingContext,
): String {
    Metrics.httpRequestTeller(pipeline.call)

    return callIdHeader.callId() ?: UUID.randomUUID().toString().also {
        logger.info("CallID ble ikke gitt på kall mot: $endpoint")
    }
}


fun NormalOpenAPIRoute.api(
    dataSource: DataSource,
    arena: IArenaoppslagRestClient,
    pdlClient: IPdlClient,
    clock: Clock = Clock.systemDefaultZone(),
) {
    tag(Tag.Perioder) {
        route("/perioder") {
            post<CallIdHeader, PerioderResponse, InternVedtakRequestApiIntern>(
                info(description = "Henter perioder med vedtak for en person innen gitte datointervall.")
            ) { callIdHeader, requestBody ->
                val callId = receiveCall("/perioder", callIdHeader, pipeline)

                sjekkTilgangTilPerson(requestBody.personidentifikator, token())

                val tilArenaKontrakt = requestBody.tilKontrakt()

                val kelvinPerioder = dataSource.transaction { connection ->
                    val behandlingsRepository = BehandlingsRepository(connection)
                    val vedtaksdata =
                        behandlingsRepository.hentVedtaksData(
                            requestBody.personidentifikator,
                            Periode(tilArenaKontrakt.fraOgMedDato, tilArenaKontrakt.tilOgMedDato)
                        )
                    perioderMedAAp(vedtaksdata)
                }

                val arenaPerioder = arena.hentPerioder(
                    callId,
                    tilArenaKontrakt
                ).perioder

                tellKildesystem(kelvinPerioder, arenaPerioder, "/perioder")

                respond(
                    PerioderResponse(
                        arenaPerioder + kelvinPerioder
                    )
                )
            }

            route("/aktivitetfase").post<CallIdHeader, PerioderInkludert11_17Response, InternVedtakRequestApiIntern>(
                info(description = "Henter perioder med vedtak fra Arena (aktivitetsfase) for en person innen gitte datointervall.")
            ) { callIdHeader, requestBody ->
                val callId = receiveCall("/perioder/aktivitetfase", callIdHeader, pipeline)

                sjekkTilgangTilPerson(requestBody.personidentifikator, token())
                val tilArenaKontrakt = requestBody.tilKontrakt()
                val arenaSvar = arena.hentPerioderInkludert11_17(callId, tilArenaKontrakt)

                tellKildesystem(
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

            route("/meldekort").post<CallIdHeader, List<Periode>, InternVedtakRequestApiIntern>(
                info(description = "Henter meldekort perioder for en person innen gitte datointerval")
            ) { _, requestBody ->
                Metrics.httpRequestTeller(pipeline.call)
                sjekkTilgangTilPerson(requestBody.personidentifikator, token())

                val perioder = dataSource.transaction { connection ->
                    val meldekortPerioderRepository = MeldekortPerioderRepository(connection)
                    meldekortPerioderRepository.hentMeldekortPerioder(requestBody.personidentifikator)
                }.filter { it.tom > requestBody.fraOgMedDato && it.fom < requestBody.tilOgMedDato }

                tellKildesystem(perioder, null, "/perioder/meldekort")

                respond(perioder, HttpStatusCode.OK)
            }
        }
    }

    tag(Tag.Meldekort) {
        route("/kelvin/meldekort-detaljer").post<CallIdHeader, MeldekortDetaljerResponse, MeldekortDetaljerRequest>(
            info(description = "Henter detaljerte meldekort for en gitt person og evt. begrenset til en gitt periode")
        ) { _, requestBody ->
            Metrics.httpRequestTeller(pipeline.call)
            val personIdentifikator = requestBody.personidentifikator
            sjekkTilgangTilPerson(personIdentifikator, token())

            val meldekortListe = dataSource.transaction { connection ->
                val meldekortService = MeldekortService(connection, pdlClient, clock)
                meldekortService.hentAlle(
                    personIdentifikator,
                    requestBody.fraOgMedDato,
                    requestBody.tilOgMedDato
                )
                    .map { (meldekort, vedtak) ->
                        meldekort.tilKontrakt(vedtak)
                    }
            }

            tellKelvinKall(pipeline.call.request)

            if (meldekortListe.isEmpty()) {
                logger.info("Fant ingen meldekort for person $personIdentifikator i den angitte perioden")
            }

            val responseBody = MeldekortDetaljerResponse(personIdentifikator, meldekortListe)
            respond(responseBody, HttpStatusCode.OK)
        }

    }

    tag(Tag.Saker) {
        // TODO: Flytt logikk til en egen service
        route("/sakerByFnr").post<CallIdHeader, List<SakStatus>, SakerRequest>(
            info(description = "Henter saker for en person")
        ) { callIdHeader, requestBody ->
            val callId = receiveCall("/sakerByFnr", callIdHeader, pipeline)

            /*
            * Listen skal kun bestå av ulike identer på samme person. Dette kontrolleres mot PDL i [hentAllePersonidenter].
            * Burde på sikt forbedre kontrollen slik at det er mindre rom for feilbruk.
            */
            sjekkTilgangTilPerson(requestBody.personidentifikatorer.first(), token())

            val personIdenter = hentAllePersonidenter(requestBody.personidentifikatorer, pdlClient)
            val kelvinSaker: List<SakStatus> =
                dataSource.transaction { connection ->
                    val sakStatusRepository = SakStatusRepository(connection)
                    personIdenter.flatMap {
                        sakStatusRepository.hentSakStatus(it)
                    }
                }
            val arenaSaker: List<SakStatus> =
                arena.hentSakerByFnr(callId, ArenaSakerRequest(personIdenter)).map {
                    arenaSakStatusTilDomene(it)
                }

            tellKildesystem(kelvinSaker, arenaSaker, "/sakerByFnr")

            respond(arenaSaker + kelvinSaker)
        }

        route("/arena/person/aap/eksisterer") {
            post<CallIdHeader, PersonEksistererIAAPArena, SakerRequest>(
                info(description = "Sjekker om en person eksisterer i AAP-arena")
            ) { callIdHeader, requestBody ->
                logger.info("Sjekker om person eksisterer i aap-arena")
                val callId = receiveCall("/arena/person/aap/eksisterer", callIdHeader, pipeline)

                pipeline.call.response.headers.append(
                    HttpHeaders.ContentType,
                    ContentType.Application.Json.withCharset(Charsets.UTF_8).toString()
                )
                respond(
                    PersonEksistererIAAPArena(
                        arena.hentPersonEksistererIAapContext(
                            callId,
                            ArenaSakerRequest(requestBody.personidentifikatorer)
                        ).eksisterer
                    )
                )
            }
        }
        route("/arena/person/aap/soknad/kan_behandles_i_kelvin") {
            post<CallIdHeader, ArenaStatusResponse, SakerRequest>(
                info(description = "Sjekker om en person kan behandles i Kelvin mtp. Arena-historikken deres")
            ) { callIdHeader, requestBody ->
                logger.info("Sjekker om personen kan behandles i Kelvin")
                val callId = receiveCall(
                    "/arena/person/aap/soknad/kan_behandles_i_kelvin",
                    callIdHeader,
                    pipeline
                )

                val arenaResponse =
                    arena.personKanBehandlesIKelvin(
                        callId,
                        ArenaSakerRequest(requestBody.personidentifikatorer)
                    )
                val response =
                    ArenaStatusResponse(arenaResponse.kanBehandles, arenaResponse.nyesteArenaSakId)

                respond(response)
            }
        }

        route("/kelvin/sakerByFnr").post<CallIdHeader, List<SakStatus>, SakerRequest>(
            info(description = "Henter saker for en person")
        ) { _, requestBody ->
            logger.info("Henter saker for en person fra Kelvin.")
            Metrics.httpRequestTeller(pipeline.call)

            /*
            * Listen skal kun bestå av ulike identer på samme person. Dette kontrolleres mot PDL i [hentAllePersonidenter].
            * Burde på sikt forbedre kontrollen slik at det er mindre rom for feilbruk.
            */
            sjekkTilgangTilPerson(requestBody.personidentifikatorer.first(), token())

            val personIdenter = hentAllePersonidenter(requestBody.personidentifikatorer, pdlClient)
            val kelvinSaker: List<SakStatus> =
                dataSource.transaction { connection ->
                    val sakStatusRepository = SakStatusRepository(connection)
                    personIdenter.flatMap {
                        sakStatusRepository.hentSakStatus(it)
                    }
                }
            tellKildesystem(kelvinSaker, null, "/kelvin/sakerByFnr")
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
                val callId = receiveCall("/maksimumUtenUtbetaling", callIdHeader, pipeline)
                val body = requestBody.tilKontrakt()
                sjekkTilgangTilPerson(requestBody.personidentifikator, token())

                val kelvinSaker: List<VedtakUtenUtbetaling> = dataSource.transaction { connection ->
                    val behandlingsRepository = BehandlingsRepository(connection)
                    VedtakService(behandlingsRepository, clock = clock).hentMediumFraKelvin(
                        requestBody.personidentifikator,
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

                tellKildesystem(kelvinSaker, arenaRespons, "/maksimumUtenUtbetaling")

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
                val callId = receiveCall("/maksimum", callIdHeader, pipeline)

                val body = requestBody.tilKontrakt()
                sjekkTilgangTilPerson(requestBody.personidentifikator, token())

                val kelvinSaker: List<Vedtak> = dataSource.transaction { connection ->
                    val behandlingsRepository = BehandlingsRepository(connection)
                    VedtakService(behandlingsRepository, clock = clock).hentMaksimum(
                        requestBody.personidentifikator,
                        Periode(body.fraOgMedDato, body.tilOgMedDato),
                    ).vedtak
                }
                val arenaVedtak = arena.hentMaksimum(callId, body).fraKontrakt().vedtak

                tellKildesystem(kelvinSaker, arenaVedtak, "/maksimum")

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
            route("maksimumUtenUtbetaling").post<CallIdHeader, Medium, InternVedtakRequestApiIntern>(
                info(description = "Henter maksimumsløsning uten utbetalinger fra kelvin for en person innen gitte datointerval. Behandlinger før 18/8 inneholder ikke beregningsgrunnlag.")
            ) { _, requestBody ->
                logger.info("Henter maksimum uten utbetalinger fra kelvin")
                Metrics.httpRequestTeller(pipeline.call)

                sjekkTilgangTilPerson(requestBody.personidentifikator, token())

                val tilArenaKontrakt = requestBody.tilKontrakt()

                val kelvinSaker: List<VedtakUtenUtbetaling> = dataSource.transaction { connection ->
                    val behandlingsRepository = BehandlingsRepository(connection)
                    VedtakService(behandlingsRepository, clock = clock).hentMediumFraKelvin(
                        requestBody.personidentifikator,
                        Periode(tilArenaKontrakt.fraOgMedDato, tilArenaKontrakt.tilOgMedDato),
                    ).vedtak
                }
                pipeline.call.response.headers.append(
                    HttpHeaders.ContentType,
                    ContentType.Application.Json.withCharset(Charsets.UTF_8).toString()
                )

                tellKildesystem(kelvinSaker, null, "/kelvin/maksimumUtenUtbetaling")

                respond(Medium(kelvinSaker))
            }

        }
    }

    tag(Tag.DSOP) {
        route("/kelvin") {
            route("/dsop") {
                route("/vedtak").post<CallIdHeader, DsopResponse, DsopRequest>(
                    info(
                        description = """Henter ut vedtaks data for en person for dsop.
                        Verdier som mangler er ikke tilgjengelige fra kelvin.
                    """.trimMargin(),
                    )
                ) { _, requestBody ->
                    logger.info("Henter vedtak fra DSOP")
                    Metrics.httpRequestTeller(pipeline.call)
                    val utrekksperiode = Periode(requestBody.fomDato, requestBody.tomDato)

                    sjekkTilgangTilPerson(requestBody.personIdent, token())

                    val kelvinVedtak = dataSource.transaction { connection ->
                        val behandlingsRepository = BehandlingsRepository(connection)
                        behandlingsRepository.hentDsopVedtak(
                            requestBody.personIdent,
                            utrekksperiode
                        )
                    }

                    respond(
                        DsopResponse(
                            utrekksperiode,
                            kelvinVedtak
                        )
                    )
                }
                route("/meldekort").post<CallIdHeader, DsopMeldekortRespons, DsopRequest>(
                    info(
                        description = """Henter ut meldekort for bruker for DSOP api"""
                    )
                ) { _, requestBody ->
                    logger.info("Henter meldekort til DSOP")
                    Metrics.httpRequestTeller(pipeline.call)
                    val utrekksperiode = Periode(requestBody.fomDato, requestBody.tomDato)

                    sjekkTilgangTilPerson(requestBody.personIdent, token())

                    val meldekortListe = dataSource.transaction { connection ->
                        val meldekortService = MeldekortService(connection, pdlClient, clock)
                        meldekortService.hentAlleMeldekort(
                            requestBody.personIdent,
                            requestBody.fomDato,
                            requestBody.tomDato
                        )
                            .map { meldekort ->
                                Meldekort(
                                    Periode(meldekort.meldePeriode.fom, meldekort.meldePeriode.tom),
                                    meldekort.arbeidPerDag.sumOf { it.timerArbeidet },
                                    meldekort.arbeidPerDag.map {
                                        TimerArbeidetPerDag(
                                            it.dag,
                                            it.timerArbeidet.toDouble()
                                        )
                                    },
                                    meldekort.mottattTidspunkt
                                )
                            }
                    }

                    respond(
                        DsopMeldekortRespons(
                            utrekksperiode,
                            meldekortListe
                        )
                    )

                }
            }
        }
    }
}

private fun tellKelvinKall(request: ApplicationRequest) {
    Metrics.kildesystemTeller("kelvin", request.path()).increment()
}

private fun tellKildesystem(
    kelvinData: List<*>?,
    arenaData: List<*>?,
    path: String,
) {
    if (!kelvinData.isNullOrEmpty()) {
        Metrics.kildesystemTeller("kelvin", path).increment()
    }

    if (!arenaData.isNullOrEmpty()) {
        Metrics.kildesystemTeller("arena", path).increment()
    }

    if (arenaData?.isNotEmpty() == true && kelvinData?.isNotEmpty() == true) {
        val toLog = arenaData.filterIsInstance<SakStatus>()
        logger.info("Fant data på person i både Kelvin og Arena på endepunkt $path. Sakstatuser fra Arena: $toLog")
    }
}

private fun sjekkTilgangTilPerson(personIdent: String, token: OidcToken) {
    if (!token.isClientCredentials()) {
        val tilgang = TilgangGateway.harTilgangTilPerson(personIdent, token)
        if (!tilgang) {
            throw IngenTilgangException("Har ikke tilgang til person")
        }
    }
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
            no.nav.aap.arenaoppslag.kontrakt.intern.Kilde.KELVIN -> Kilde.KELVIN
        }
    )

private fun hentAllePersonidenter(
    identerFraRequest: List<String>,
    pdlClient: IPdlClient,
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

fun utledVedtakStatus(
    behandlingStatus: KelvinBehandlingStatus,
    sakStatus: KelvinSakStatus,
    periode: Periode,
    nå: LocalDate = LocalDate.now(),
): String =
    if (
        (behandlingStatus.iverksatt() && sakStatus != KelvinSakStatus.AVSLUTTET) ||
        periode.tom.isAfter(nå) ||
        sakStatus != KelvinSakStatus.AVSLUTTET
    ) {
        Status.LØPENDE.toString()
    } else if (behandlingStatus == KelvinBehandlingStatus.AVSLUTTET) {
        Status.AVSLUTTET.toString()
    } else {
        Status.UTREDES.toString()
    }


fun InternVedtakRequestApiIntern.tilKontrakt(): InternVedtakRequest {
    return InternVedtakRequest(
        personidentifikator = personidentifikator,
        fraOgMedDato = fraOgMedDato ?: LocalDate.of(1, 1, 1),
        tilOgMedDato = tilOgMedDato ?: LocalDate.of(9999, 12, 31)
    )
}

class IngenTilgangException(message: String) : RuntimeException(message)
