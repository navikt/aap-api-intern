package api

import api.arena.IArenaoppslagRestClient
import api.pdl.IPdlClient
import api.postgres.*
import api.util.fraKontrakt
import api.util.fraKontraktUtenUtbetaling
import api.util.perioderMedAAp
import com.papsign.ktor.openapigen.APITag
import com.papsign.ktor.openapigen.annotations.parameters.HeaderParam
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
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.api.intern.*
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.audience
import no.nav.aap.komponenter.httpklient.auth.token
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.miljo.Miljø
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
    @HeaderParam("callId") val `Nav-CallId`: String? = null,
    @HeaderParam("correlation id") val `X-Correlation-Id`: String? = null,
) {
    fun callId(): String? {
        val callId = listOfNotNull(`Nav-CallId`, `X-Correlation-Id`).firstOrNull()

        return callId
    }
}

enum class Tag(override val description: String) : APITag {
    Perioder("For å hente perioder med AAP"),
    Saker("For å hente AAP-saker"),
    Maksimum("For å hente maksimumsløsning")
}

fun NormalOpenAPIRoute.api(
    dataSource: DataSource,
    arena: IArenaoppslagRestClient,
    httpCallCounter: PrometheusMeterRegistry,
    pdlClient: IPdlClient,
) {
    tag(Tag.Perioder) {
        route("/perioder") {
            post<CallIdHeader, PerioderResponse, InternVedtakRequest>(
                info(description = "Henter perioder med vedtak for en person innen gitte datointerval")
            ) { callIdHeader, requestBody ->
                val azpName = azpName()
                httpCallCounter.httpCallCounter(
                    "/perioder",
                    pipeline.call.audience(),
                    azpName ?: ""
                ).increment()
                val callId = callIdHeader.callId() ?: UUID.randomUUID().toString().also {
                    logger.info("CallID ble ikke gitt på kall mot: /perioder")
                }

                if (!harTilgangTilPerson(requestBody.personidentifikator, token())) {
                    respondWithStatus(HttpStatusCode.Forbidden)
                }

                val kelvinPerioder = dataSource.transaction { connection ->
                    val behandlingsRepository = BehandlingsRepository(connection)
                    val vedtaksdata =
                        behandlingsRepository.hentVedtaksData(requestBody.personidentifikator)
                    perioderMedAAp(
                        vedtaksdata
                    )
                }
                respond(
                    PerioderResponse(
                        arena.hentPerioder(
                            callId,
                            requestBody
                        ).perioder + kelvinPerioder
                    )
                )
            }

            route("/aktivitetfase").post<CallIdHeader, PerioderInkludert11_17Response, InternVedtakRequest>(
                info(description = "Henter perioder med vedtak (inkl. aktivitetsfase) for en person innen gitte datointerval")
            ) { callIdHeader, requestBody ->
                httpCallCounter.httpCallCounter(
                    "/perioder/aktivitetfase",
                    pipeline.call.audience(),
                    azpName() ?: ""
                )
                    .increment()
                val callId = callIdHeader.callId() ?: UUID.randomUUID().toString().also {
                    logger.info("CallID ble ikke gitt på kall mot: /perioder/aktivitetfase")
                }

                if (!harTilgangTilPerson(requestBody.personidentifikator, token())) {
                    respondWithStatus(HttpStatusCode.Forbidden)
                }

                val arenaSvar = arena.hentPerioderInkludert11_17(callId, requestBody)

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
            route("/meldekort").post<CallIdHeader, List<Periode>, InternVedtakRequest>(
                info(description = "Henter meldekort perioder for en person innen gitte datointerval")
            ) { callIdHeader, requestBody ->

                if (!harTilgangTilPerson(requestBody.personidentifikator, token())) {
                    respondWithStatus(HttpStatusCode.Forbidden)
                }
                val perioder = dataSource.transaction { connection ->
                    val meldekortPerioderRepository = MeldekortPerioderRepository(connection)
                    meldekortPerioderRepository.hentMeldekortPerioder(requestBody.personidentifikator)
                }.filter { it.tom > requestBody.fraOgMedDato && it.fom < requestBody.tilOgMedDato }
                respond(perioder, HttpStatusCode.OK)
            }
        }
    }

    tag(Tag.Saker) {
        // TODO: Flytt logikk til en egen service
        route("/sakerByFnr").post<CallIdHeader, List<SakStatus>, SakerRequest>(
            info(description = "Henter saker for en person")
        ) { callIdHeader, requestBody ->
            httpCallCounter.httpCallCounter(
                "/sakerByFnr",
                pipeline.call.audience(),
                azpName() ?: ""
            ).increment()

            val callId = callIdHeader.callId() ?: UUID.randomUUID().toString().also {
                logger.info("CallID ble ikke gitt på kall mot: /sakerByFnr")
            }

            if (!harTilgangTilPerson(requestBody.personidentifikatorer.first(), token())) {
                respondWithStatus(HttpStatusCode.Forbidden)
            }

            val personIdenter = hentAllePersonidenter(requestBody.personidentifikatorer, pdlClient)
            val kelvinSaker: List<SakStatus> = dataSource.transaction { connection ->
                val sakStatusRepository = SakStatusRepository(connection)
                personIdenter.flatMap {
                    sakStatusRepository.hentSakStatus(it)
                }
            }
            val arenaSaker = arena.hentSakerByFnr(callId, SakerRequest(personIdenter)).map {
                arenaSakStatusTilDomene(it)
            }
            respond(arenaSaker + kelvinSaker)
        }

        route("/arena/person/aap/eksisterer") {
            post<CallIdHeader, PersonEksistererIAAPArena, SakerRequest>(
                info(description = "Sjekker om en person eksisterer i AAP-arena")
            ) { callIdHeader, requestBody ->
                httpCallCounter.httpCallCounter(
                    "/arena/person/aap/eksisterer",
                    pipeline.call.audience(),
                    azpName() ?: ""
                ).increment()
                val callId = callIdHeader.callId() ?: UUID.randomUUID().toString().also {
                    logger.info("CallID ble ikke gitt på kall mot: /person/aap/eksisterer")
                }

                pipeline.call.response.headers.append(HttpHeaders.ContentType, ContentType.Application.Json.withCharset(Charsets.UTF_8).toString())
                respond(
                    PersonEksistererIAAPArena(
                        arena.hentPersonEksistererIAapContext(
                            callId,
                            requestBody
                        ).eksisterer
                    )
                )
            }
        }
    }

    tag(Tag.Maksimum) {
        route("/maksimumUtenUtbetaling") {
            post<CallIdHeader, Medium, InternVedtakRequest>(
                info(description = "Henter maksimumsløsning uten utbetalinger for en person innen gitte datointerval")
            ) { callIdHeader, requestBody ->
                httpCallCounter.httpCallCounter(
                    "/maksimumUtenUtbetaling",
                    pipeline.call.audience(),
                    azpName() ?: ""
                ).increment()
                val callId = callIdHeader.callId() ?: UUID.randomUUID().toString().also {
                    logger.info("CallID ble ikke gitt på kall mot: /maksimum")
                }

                if (!harTilgangTilPerson(requestBody.personidentifikator, token())) {
                    respondWithStatus(HttpStatusCode.Forbidden)
                }

                val kelvinSaker: List<VedtakUtenUtbetaling> = dataSource.transaction { connection ->
                    val behandlingsRepository = BehandlingsRepository(connection)
                    hentMediumFraKelvin(
                        requestBody.personidentifikator,
                        behandlingsRepository
                    ).vedtak
                }
                pipeline.call.response.headers.append(HttpHeaders.ContentType, ContentType.Application.Json.withCharset(Charsets.UTF_8).toString())
                respond(
                    Medium(
                        arena.hentMaksimum(
                            callId,
                            requestBody
                        ).vedtak.map { it.fraKontraktUtenUtbetaling() } + kelvinSaker
                    )
                )
            }
        }
        route("/maksimum") {
            post<CallIdHeader, Maksimum, InternVedtakRequest>(
                info(description = "Henter maksimumsløsning for en person innen gitte datointerval")
            ) { callIdHeader, requestBody ->
                httpCallCounter.httpCallCounter(
                    "/maksimum",
                    pipeline.call.audience(),
                    azpName() ?: ""
                ).increment()
                val callId = callIdHeader.callId() ?: UUID.randomUUID().toString().also {
                    logger.info("CallID ble ikke gitt på kall mot: /maksimum")
                }
                
                if (!harTilgangTilPerson(requestBody.personidentifikator, token())) {
                    respondWithStatus(HttpStatusCode.Forbidden)
                }

                val kelvinSaker: List<Vedtak> = dataSource.transaction { connection ->
                    val behandlingsRepository = BehandlingsRepository(connection)
                    behandlingsRepository.hentMaksimum(
                        requestBody.personidentifikator,
                        Periode(requestBody.fraOgMedDato, requestBody.tilOgMedDato)
                    ).vedtak
                }
                pipeline.call.response.headers.append(HttpHeaders.ContentType, ContentType.Application.Json.withCharset(Charsets.UTF_8).toString())
                respond(
                    Maksimum(
                        arena.hentMaksimum(callId, requestBody).fraKontrakt().vedtak + kelvinSaker
                    )
                )
            }
        }
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

    val identerFraPdl =  pdlClient.hentAlleIdenterForPerson(identerFraRequest.first()).map { pdlIdent ->
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

fun hentMediumFraKelvin(
    fnr: String,
    behandlingsRepository: BehandlingsRepository
): Medium {
    val kelvinData = behandlingsRepository.hentVedtaksData(fnr)
    val vedtak: List<VedtakUtenUtbetaling> = kelvinData.flatMap { behandling ->
        val rettighetsTypeTidslinje = Tidslinje(
            behandling.rettighetsTypeTidsLinje.map {
                Segment(
                    Periode(it.fom, it.tom),
                    it.verdi
                )
            }
        )

        val tilkjent = Tidslinje(
            behandling.tilkjent.map {
                Segment(
                    Periode(it.tilkjentFom, it.tilkjentTom),
                    TilkjentDB(
                        it.dagsats,
                        it.grunnlag,
                        it.gradering,
                        it.grunnlagsfaktor,
                        it.grunnbeløp,
                        it.antallBarn,
                        it.barnetilleggsats,
                        it.barnetillegg
                    )
                )
            }
        )

        rettighetsTypeTidslinje.kombiner(
            tilkjent,
            JoinStyle.LEFT_JOIN { periode, left, right ->
                Segment(
                    periode,
                    VedtakUtenUtbetalingUtenPeriode(
                        vedtakId = behandling.vedtakId.toString(),
                        dagsats = right?.verdi?.dagsats ?: 0,
                        status = utledVedtakStatus(behandling.behandlingStatus, behandling.sak.status, periode),
                        saksnummer = behandling.sak.saksnummer,
                        vedtaksdato = behandling.vedtaksDato,
                        vedtaksTypeKode = null,
                        vedtaksTypeNavn = null,
                        rettighetsType = left.verdi,
                        beregningsgrunnlag = right?.verdi?.grunnlag?.toInt()?.times(260) ?: 0,
                        barnMedStonad = right?.verdi?.antallBarn ?: 0,
                        kildesystem = Kilde.KELVIN.toString(),
                        samordningsId = behandling.samId,
                        opphorsAarsak = null
                    )
                )
            }
        ).komprimer()
            .map {
                it.verdi.tilVedtakUtenUtbetaling(
                    no.nav.aap.api.intern.Periode(
                        it.periode.fom,
                        it.periode.tom
                    )
                )
            }
            .filter { (it.status == Status.LØPENDE.toString() || it.status == Status.AVSLUTTET.toString()) }
    }

    return Medium(vedtak)
}

fun utledVedtakStatus(
    behandlingStatus: no.nav.aap.behandlingsflyt.kontrakt.behandling.Status,
    sakStatus: Status,
    periode: Periode
): String =
    if (
        behandlingStatus == no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.IVERKSETTES ||
        periode.tom.isAfter(LocalDate.now()) ||
        sakStatus != Status.AVSLUTTET
    ) {
        Status.LØPENDE.toString()
    } else if (behandlingStatus == no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.AVSLUTTET) {
        Status.AVSLUTTET.toString()
    } else {
        Status.UTREDES.toString()
    }