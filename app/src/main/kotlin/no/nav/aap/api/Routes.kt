package no.nav.aap.api

import com.papsign.ktor.openapigen.APITag
import com.papsign.ktor.openapigen.annotations.parameters.HeaderParam
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.properties.description.Description
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import com.papsign.ktor.openapigen.route.tags
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import no.nav.aap.api.arena.ArenaService
import no.nav.aap.api.dsop.dsopRoutes
import no.nav.aap.api.intern.*
import no.nav.aap.api.kelvin.AktivitetsfaseService
import no.nav.aap.api.kelvin.KelvinSakService
import no.nav.aap.api.kelvin.VedtakService
import no.nav.aap.api.maksimum.InternVedtak
import no.nav.aap.api.maksimum.InternVedtakUtenUtbetaling
import no.nav.aap.api.maksimum.tilKontrakt
import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.api.postgres.MeldekortPerioderRepository
import no.nav.aap.api.postgres.SakStatusRepository
import no.nav.aap.api.util.perioderMedAAp
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.server.auth.token
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tilgang.AuthorizationMachineToMachineConfig
import no.nav.aap.tilgang.authorizedPost
import no.nav.aap.tilgang.plugin.kontrakt.Personreferanse
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalDate
import java.util.*
import javax.sql.DataSource

private val logger = LoggerFactory.getLogger("App")

@Suppress("PropertyName")
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
    NKS("Endepunkter brukt av NKS"),
    Meldekort("For å hente AAP-meldekort"),
    Maksimum("For å hente maksimumsløsning"),
    DSOP("For DSOP-relaterte endepunkter"),
    OBO("Endepunkter ment for Team OBO."),
    ArenaHistorikk("For å hente informasjon om AAP historikk fra Arena"),
    Syfo("Endepunkter brukt av Syfo"),
    ;
}

data class SakerRequest(
    @property:Description("Liste med personidentifikatorer. Må svare til samme person.")
    val personidentifikatorer: List<String>,
) : Personreferanse {
    override fun hentPersonreferanse() = personidentifikatorer.first()
}

data class SakerRequestMeldekortbackend(
    @property:Description("Personidentifikator")
    val personidentifikator: String
)

data class ArenaSakParameter(
    @param:PathParam("sakId") val sakId: String,
    @Suppress("PropertyName") @param:HeaderParam("Nav-CallId") val `Nav-CallId`: String? = null,
    @Suppress("PropertyName") @param:HeaderParam("X-Correlation-Id") val `X-Correlation-Id`: String? = null,
) {
    fun callId(): String? = listOfNotNull(`Nav-CallId`, `X-Correlation-Id`).firstOrNull()
}

internal fun receiveCall(
    callIdHeader: CallIdHeader,
    pipeline: RoutingContext,
): String {
    Metrics.httpRequestTeller(pipeline.call)

    return callIdHeader.callId() ?: UUID.randomUUID().toString().also {
        logger.info("CallID ble ikke gitt på kall mot: ${pipeline.call.request.path()}")
    }
}


fun NormalOpenAPIRoute.api(
    dataSource: DataSource,
    arenaService: ArenaService,
    pdlGateway: IPdlGateway,
    clock: Clock = Clock.systemDefaultZone(),
) {

    tag(Tag.Perioder) {
        route("/perioder") {
            post<CallIdHeader, PerioderResponse, InternVedtakRequestApiIntern>(
                info(description = "Henter perioder med vedtak for en person innen gitte datointervall.")
            ) { callIdHeader, requestBody ->
                val callId = receiveCall(callIdHeader, pipeline)

                sjekkTilgangTilPerson(requestBody.personidentifikator, token())

                val vedtakRequest = requestBody.tilKontrakt()

                val kelvinPerioder = dataSource.transaction { connection ->
                    val behandlingsRepository = BehandlingsRepository(connection)
                    val vedtaksdata =
                        behandlingsRepository.hentVedtaksData(
                            requestBody.personidentifikator,
                            Periode(vedtakRequest.fraOgMedDato, vedtakRequest.tilOgMedDato)
                        )
                    perioderMedAAp(vedtaksdata)
                }

                val arenaPerioder = arenaService.hentPerioder(callId, vedtakRequest)

                tellKildesystem(kelvinPerioder, arenaPerioder, "/perioder")

                respond(
                    PerioderResponse(
                        arenaPerioder + kelvinPerioder
                    )
                )
            }

            route("/aktivitetfase").authorizedPost<CallIdHeader, PerioderInkludert11_17Response, InternVedtakRequestApiIntern>(
                AuthorizationMachineToMachineConfig(
                    authorizedAzps = listOf(
                        UUID.fromString(
                            requiredConfigForKey("AZP_TILLEGGSSTONADER_INTEGRASJONER")
                        )
                    ) + azpForTokenGenHvisIkkeProd()
                ), null, null, null,
                info(description = "Henter perioder med vedtak fra Arena (aktivitetsfase) for en person innen gitte datointervall. Er ment for Team Tilleggstønader.")
            ) { callIdHeader, requestBody ->
                val callId = receiveCall(callIdHeader, pipeline)

                sjekkTilgangTilPerson(requestBody.personidentifikator, token())
                val vedtakRequest = requestBody.tilKontrakt()
                val aktfaseKelvin = dataSource.transaction { connection ->
                    val aktivitetsfaseService = AktivitetsfaseService(connection)
                    aktivitetsfaseService.hentPerioderMedAktivitetsfase(
                        vedtakRequest.personidentifikator,
                        Periode(vedtakRequest.fraOgMedDato, vedtakRequest.tilOgMedDato)
                    )
                }
                val aktivitetfase = arenaService.aktivitetfase(callId, vedtakRequest)

                tellKildesystem(
                    aktfaseKelvin,
                    aktivitetfase.perioder,
                    "/perioder/aktivitetfase"
                )

                respond(PerioderInkludert11_17Response(aktivitetfase.perioder + aktfaseKelvin))
            }

            route("/meldekort").post<CallIdHeader, List<Periode>, InternVedtakRequestApiIntern>(
                info(description = "Henter meldekort perioder for en person innen gitte datointerval")
            ) { _, requestBody ->
                Metrics.httpRequestTeller(pipeline.call)
                sjekkTilgangTilPerson(requestBody.personidentifikator, token())
                val konsument = pipeline.call.principal<JWTPrincipal>()?.let {
                    it.payload.claims["azp_name"]?.asString()
                } ?: ""
                logger.info("Kaller /perioder/meldekort. Konsument: $konsument")
                val perioder = dataSource.transaction { connection ->
                    val meldekortPerioderRepository = MeldekortPerioderRepository(connection)
                    meldekortPerioderRepository.hentMeldekortPerioder(requestBody.personidentifikator)
                }.filter { it.tom > requestBody.fraOgMedDato && it.fom < requestBody.tilOgMedDato }

                tellKildesystem(perioder, null, "/perioder/meldekort")

                respond(perioder, HttpStatusCode.OK)
            }
        }
    }

    nksRoutes(dataSource, arenaService, pdlGateway, clock)

    tag(Tag.Saker) {
        route("/meldekort-backend/sakerByFnr").authorizedPost<CallIdHeader, List<SakStatusMeldekortbackend>, SakerRequestMeldekortbackend>(
            AuthorizationMachineToMachineConfig(
                authorizedAzps = listOf(
                    UUID.fromString(
                        requiredConfigForKey("AZP_MELDEKORT_BACKEND")
                    )
                ) + azpForTokenGenHvisIkkeProd()
            ),
            null, null, null,
            info(description = "Henter saker for en person. Kan kun kalles fra meldekort-backend.")
        ) { callIdHeader, requestBody ->
            val callId = receiveCall(callIdHeader, pipeline)

            val personIdenter =
                pdlGateway.hentAlleIdenterForPerson(requestBody.personidentifikator)
                    .map { pdlIdent -> pdlIdent.ident }

            val kelvinSaker: List<SakStatusMeldekortbackend> =
                dataSource.transaction { connection ->
                    val kelvinSakService = KelvinSakService(
                        SakStatusRepository(connection),
                        BehandlingsRepository(connection)
                    )

                    kelvinSakService.hentSakStatusUtenEnhet(personIdenter)
                }
            val arenaSaker: List<SakStatusMeldekortbackend> =
                arenaService.hentSaker(callId, listOf(requestBody.personidentifikator))
                    .map { SakStatusMeldekortbackend(it.kilde, it.periode(), it.sakId) }

            tellKildesystem(kelvinSaker, arenaSaker, "/sakerByFnr")

            respond(arenaSaker + kelvinSaker)
        }

        route("/kelvin/sakerByFnr").post<CallIdHeader, List<SakStatusOverlappskontroll>, SakerRequest>(
            info(description = "Henter saker for en person. Brukes av Arena for overlappskontroll. Hvis flere konsumenter ønsker dette, dupliser endepunktet.")
        ) { _, requestBody ->
            Metrics.httpRequestTeller(pipeline.call)

            /*
            * Listen skal kun bestå av ulike identer på samme person. Dette kontrolleres mot PDL i [hentAllePersonidenter].
            * Burde på sikt forbedre kontrollen slik at det er mindre rom for feilbruk.
            */
            sjekkTilgangTilPerson(requestBody.personidentifikatorer.first(), token())
            Metrics.antallIdenter("/kelvin/sakerByFnr", requestBody.personidentifikatorer.size)

            val personIdenter = hentAllePersonidenter(requestBody.personidentifikatorer, pdlGateway)
            val kelvinSaker: List<SakStatus.Kelvin> =
                dataSource.transaction { connection ->
                    val sakStatusRepository = SakStatusRepository(connection)
                    val kelvinSakService =
                        KelvinSakService(sakStatusRepository, BehandlingsRepository(connection))

                    kelvinSakService.hentSakStatus(personIdenter)
                }
            tellKildesystem(kelvinSaker, null, "/kelvin/sakerByFnr")
            respond(kelvinSaker.map {
                SakStatusOverlappskontroll(
                    sakId = it.sakId,
                    statusKode = it.status(),
                    periode = it.periode,
                    // Dette kommer fra Kelvin-periode, som alltid har ikke-null fra-dato
                    fraDato = it.periode.fraOgMedDato!!,
                    kilde = it.kilde,
                    enhet = it.enhet
                )
            })
        }
    }
    tag(Tag.ArenaHistorikk) {
        route("/arena/person/aap/eksisterer") {
            post<CallIdHeader, PersonEksistererIAAPArena, SakerRequest>(
                info(description = "Sjekker om en person eksisterer i Arena (AAP).")
            ) { callIdHeader, requestBody ->
                logger.info("Sjekker om person eksisterer i aap-arena")
                val callId = receiveCall(callIdHeader, pipeline)

                pipeline.call.response.headers.append(
                    HttpHeaders.ContentType,
                    ContentType.Application.Json.withCharset(Charsets.UTF_8).toString()
                )
                val eksistererIAAPArena =
                    arenaService.eksistererIAapArena(callId, requestBody.personidentifikatorer)
                respond(eksistererIAAPArena)
            }
        }
        route("/arena/person/saker") {
            post<CallIdHeader, ArenaSakerResponse, ArenaSakerRequest>(
                info(description = "Henter saker for en person fra Arena via ny v1-kontrakt.")
            ) { callIdHeader, requestBody ->
                logger.info("Henter saker for person fra Arena (v1)")
                val callId = receiveCall(callIdHeader, pipeline)
                sjekkTilgangTilPerson(requestBody.personidentifikator, token())
                val saker = arenaService.hentSakerForPerson(callId, requestBody.personidentifikator)
                respond(saker)
            }
        }
        route("/arena/sak/{sakId}") {
            get<ArenaSakParameter, ArenaSakMedVedtakResponse>(
                info(description = "Henter en Arena-sak med tilhørende vedtak.")
            ) { params ->
                val callId = params.callId() ?: UUID.randomUUID().toString().also {
                    logger.info("CallID ble ikke gitt på kall mot: ${pipeline.call.request.path()}")
                }
                Metrics.httpRequestTeller(pipeline.call)

                val sak = arenaService.hentArenaSakMedVedtak(callId, params.sakId)
                    ?: return@get pipeline.call.respond(HttpStatusCode.NotFound)

                sjekkTilgangTilPerson(sak.person.fodselsnummer, token())

                respond(sak)
            }
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
                val callId = receiveCall(callIdHeader, pipeline)
                val body = requestBody.tilKontrakt()
                sjekkTilgangTilPerson(requestBody.personidentifikator, token())

                val kelvinSaker: List<InternVedtakUtenUtbetaling> = dataSource.transaction { connection ->
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

                val arenaSaker = arenaService.hentVedtakUtenUtbetaling(callId, body)

                tellKildesystem(kelvinSaker, arenaSaker, "/maksimumUtenUtbetaling")

                respond(Medium((arenaSaker + kelvinSaker).map { it.tilKontrakt() }))
            }
        }
        route("/maksimum") {
            post<CallIdHeader, Maksimum, InternVedtakRequestApiIntern>(
                info(
                    description = """
                    Henter maksimumsløsning for en person innen gitte datointervall. Behandlinger før 18/8 inneholder ikke beregningsgrunnlag.
                    dagsatsEtterUføreReduksjon er kun tilgjengelig fra Kelvin.
                    """.trimIndent()
                )
            ) { callIdHeader, requestBody ->
                logger.info("Henter maksimum")
                val callId = receiveCall(callIdHeader, pipeline)

                val body = requestBody.tilKontrakt()
                sjekkTilgangTilPerson(requestBody.personidentifikator, token())

                val kelvinSaker: List<InternVedtak> = dataSource.transaction { connection ->
                    val behandlingsRepository = BehandlingsRepository(connection)
                    VedtakService(behandlingsRepository, clock = clock).hentMaksimum(
                        requestBody.personidentifikator,
                        Periode(body.fraOgMedDato, body.tilOgMedDato),
                    ).vedtak
                }
                val arenaVedtak = arenaService.hentVedtak(callId, body)

                tellKildesystem(kelvinSaker, arenaVedtak, "/maksimum")

                pipeline.call.response.headers.append(
                    HttpHeaders.ContentType,
                    ContentType.Application.Json.withCharset(Charsets.UTF_8).toString()
                )

                respond(
                    Maksimum((arenaVedtak + kelvinSaker).map { it.tilKontrakt() })
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

                val request = requestBody.tilKontrakt()

                val kelvinSaker: List<InternVedtakUtenUtbetaling> = dataSource.transaction { connection ->
                    val behandlingsRepository = BehandlingsRepository(connection)
                    VedtakService(behandlingsRepository, clock = clock).hentMediumFraKelvin(
                        requestBody.personidentifikator,
                        Periode(request.fraOgMedDato, request.tilOgMedDato),
                    ).vedtak
                }
                pipeline.call.response.headers.append(
                    HttpHeaders.ContentType,
                    ContentType.Application.Json.withCharset(Charsets.UTF_8).toString()
                )

                tellKildesystem(kelvinSaker, null, "/kelvin/maksimumUtenUtbetaling")

                respond(Medium(kelvinSaker.map { it.tilKontrakt() }))
            }
        }
    }

    route("/kelvin/") {
        route("obo").authorizedPost<CallIdHeader, ResponsTilTeamObo, InternVedtakRequestApiIntern>(
            AuthorizationMachineToMachineConfig(
                authorizedAzps = listOf(
                    UUID.fromString(
                        requiredConfigForKey("AZP_VEILARBOPPFOLGING")
                    )
                ) + azpForTokenGenHvisIkkeProd()
            ),
            auditLogConfig = null,
            exampleResponse = ResponsTilTeamObo(
                vedtak = listOf(
                    VedtakTeamObo(
                        status = "LØPENDE",
                        saksnummer = "4MGL8LS",
                        vedtaksdato = LocalDate.now(),
                        periode = Periode(
                            fraOgMedDato = LocalDate.of(2021, 1, 1),
                            tilOgMedDato = LocalDate.of(2021, 1, 31)
                        ),
                        rettighetsType = "BISTANDSBEHOV",
                    )
                ),
                sakstatus = KelvinStatus.FERDIGBEHANDLET,
                maksdato = LocalDate.of(2021, 2, 1)
            ),
            exampleRequest = null,
            info(description = "Endepunkt Team OBO."),
            tags(Tag.OBO),

            ) { _, requestBody ->
            Metrics.httpRequestTeller(pipeline.call)

            sjekkTilgangTilPerson(requestBody.personidentifikator, token())

            val request = requestBody.tilKontrakt()

            val (kelvinSaker, sakStatus) = dataSource.transaction { connection ->
                val behandlingsRepository = BehandlingsRepository(connection)

                val sakstatus = KelvinSakService(
                    SakStatusRepository(connection),
                    BehandlingsRepository(connection)
                ).hentSakStatus(listOf(requestBody.personidentifikator)).firstOrNull()

                Pair(
                    VedtakService(behandlingsRepository, clock = clock).hentMediumFraKelvin(
                        requestBody.personidentifikator,
                        Periode(request.fraOgMedDato, request.tilOgMedDato),
                    ).vedtak, sakstatus
                )
            }

            if (sakStatus == null) {
                pipeline.call.respond(
                    HttpStatusCode.NotFound,
                    "Fant ingen sakstatus for personen i Kelvin"
                )
                return@authorizedPost
            }

            tellKildesystem(kelvinSaker, null, "/kelvin/maksimumUtenUtbetaling")

            respond(
                ResponsTilTeamObo(
                    kelvinSaker.map {
                        VedtakTeamObo(
                            status = it.status,
                            saksnummer = it.saksnummer,
                            vedtaksdato = it.vedtaksdato,
                            periode = it.periode.tilKontrakt(),
                            rettighetsType = it.rettighetsType,
                        )
                    },
                    sakstatus = sakStatus.status(),
                    maksdato = sakStatus.forelopigMaksdato
                )
            )
        }

    }

    tag(Tag.DSOP) {
        route("/kelvin/dsop") {
            dsopRoutes(dataSource, pdlGateway, clock)
        }
    }
}

internal fun azpForTokenGenHvisIkkeProd(): List<UUID> =
    if (!Miljø.erProd()) listOf(UUID.fromString(requiredConfigForKey("AZP_TOKEN_GEN"))) else emptyList()

internal fun tellKelvinKall(request: ApplicationRequest) {
    Metrics.kildesystemTeller("kelvin", request.path()).increment()
}

internal fun tellKildesystem(
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

fun sjekkTilgangTilPerson(personIdent: String, token: OidcToken) {
    if (!token.isClientCredentials()) {
        Metrics.tokentype("obo")
        val tilgang = TilgangGateway.harTilgangTilPerson(personIdent, token)
        if (!tilgang) {
            throw IngenTilgangException("Har ikke tilgang til person")
        }
    } else {
        Metrics.tokentype("m2m")
    }
}


internal fun hentAllePersonidenter(
    identerFraRequest: List<String>,
    pdlClient: IPdlGateway,
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


fun InternVedtakRequestApiIntern.tilKontrakt(): InternVedtakRequest {
    return InternVedtakRequest(
        personidentifikator = personidentifikator,
        fraOgMedDato = fraOgMedDato ?: LocalDate.of(1, 1, 1),
        tilOgMedDato = tilOgMedDato ?: LocalDate.of(9999, 12, 31)
    )
}

class IngenTilgangException(message: String) : RuntimeException(message)

val Periode.somDTO: PeriodeDTO get() = PeriodeDTO(fom, tom)
