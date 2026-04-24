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
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import no.nav.aap.api.arena.ArenaService
import no.nav.aap.api.dsop.dsopRoutes
import no.nav.aap.api.intern.*
import no.nav.aap.api.kelvin.*
import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.api.postgres.MeldekortPerioderRepository
import no.nav.aap.api.postgres.SakStatusRepository
import no.nav.aap.api.util.perioderMedAAp
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerRequest
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.server.auth.token
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tilgang.AuthorizationMachineToMachineConfig
import no.nav.aap.tilgang.authorizedPost
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
    Meldekort("For å hente AAP-meldekort"),
    Maksimum("For å hente maksimumsløsning"),
    DSOP("For DSOP-relaterte endepunkter"),
    ArenaHistorikk("For å hente informasjon om AAP historikk fra Arena"), ;
}

data class SakerRequest(
    @param:Description("Liste med personidentifikatorer. Må svare til samme person.")
    val personidentifikatorer: List<String>,
)

data class SakerRequestMeldekortbackend(
    @param:Description("Personidentifikator")
    val personidentifikator: String
)

private fun receiveCall(
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
                ), null,
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

    tag(Tag.Meldekort) {
        // Begrenset til kun for NKS. Dupliser hvis nye konsumenter trenger samme data.
        // Muligens ikke i bruk.
        route("/kelvin/meldekort-detaljer").authorizedPost<CallIdHeader, MeldekortDetaljerResponse, MeldekortDetaljerRequest>(
            AuthorizationMachineToMachineConfig(
                authorizedAzps = listOf(
                    UUID.fromString(
                        requiredConfigForKey("AZP_SAAS_PROXY")
                    )
                ) + azpForTokenGenHvisIkkeProd()
            ), null,
            info(description = "Henter detaljerte meldekort for en gitt person og evt. begrenset til en gitt periode. Kan kun brukes av NKS.")
        ) { _, requestBody ->
            Metrics.httpRequestTeller(pipeline.call)
            val personIdentifikator = requestBody.personidentifikator
            sjekkTilgangTilPerson(personIdentifikator, token())

            val meldekortListe = dataSource.transaction { connection ->
                val meldekortService = MeldekortService(connection, pdlGateway, clock)
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
        // Begrenset til kun for NKS. Dupliser hvis nye konsumenter trenger samme data.
        route("/sakerByFnr").authorizedPost<CallIdHeader, List<SakStatus>, SakerRequest>(
            AuthorizationMachineToMachineConfig(
                authorizedAzps = listOf(
                    UUID.fromString(
                        requiredConfigForKey("AZP_SAAS_PROXY")
                    )
                ) + azpForTokenGenHvisIkkeProd()
            ),
            null, info(description = "Endepunkt ment kun for NKS. Henter saker for en person.")
        ) { callIdHeader, requestBody ->
            val callId = receiveCall(callIdHeader, pipeline)

            /*
            * Listen skal kun bestå av ulike identer på samme person. Dette kontrolleres mot PDL i [hentAllePersonidenter].
            * Burde på sikt forbedre kontrollen slik at det er mindre rom for feilbruk.
            */
            sjekkTilgangTilPerson(requestBody.personidentifikatorer.first(), token())
            Metrics.antallIdenter("/sakerByFnr", requestBody.personidentifikatorer.size)
            if (!token().isClientCredentials()) {
                logger.info("Token er client credentials, sjekker ikke tilgang.")
            }

            val personIdenter = hentAllePersonidenter(requestBody.personidentifikatorer, pdlGateway)
            val kelvinSaker: List<SakStatus> =
                dataSource.transaction { connection ->
                    val kelvinSakService = KelvinSakService(SakStatusRepository(connection))

                    kelvinSakService.hentSakStatus(personIdenter)
                }
            val arenaSaker: List<SakStatus> =
                arenaService.hentSaker(callId, requestBody.personidentifikatorer)


            tellKildesystem(kelvinSaker, arenaSaker, "/sakerByFnr")

            respond(arenaSaker + kelvinSaker)
        }

        route("/meldekort-backend/sakerByFnr").authorizedPost<CallIdHeader, List<SakStatusMeldekortbackend>, SakerRequestMeldekortbackend>(
            AuthorizationMachineToMachineConfig(
                authorizedAzps = listOf(
                    UUID.fromString(
                        requiredConfigForKey("AZP_MELDEKORT_BACKEND")
                    )
                ) + azpForTokenGenHvisIkkeProd()
            ),
            null,
            info(description = "Henter saker for en person. Kan kun kalles fra meldekort-backend.")
        ) { callIdHeader, requestBody ->
            val callId = receiveCall(callIdHeader, pipeline)

            val personIdenter =
                pdlGateway.hentAlleIdenterForPerson(requestBody.personidentifikator)
                    .map { pdlIdent -> pdlIdent.ident }

            val kelvinSaker: List<SakStatusMeldekortbackend> =
                dataSource.transaction { connection ->
                    val kelvinSakService = KelvinSakService(SakStatusRepository(connection))

                    kelvinSakService.hentSakStatusUtenEnhet(personIdenter)
                }
            val arenaSaker: List<SakStatusMeldekortbackend> =
                arenaService.hentSaker(callId, listOf(requestBody.personidentifikator))
                    .map { SakStatusMeldekortbackend(it.kilde, it.periode, it.sakId) }

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
            val kelvinSaker: List<SakStatus> =
                dataSource.transaction { connection ->
                    val sakStatusRepository = SakStatusRepository(connection)
                    val kelvinSakService = KelvinSakService(sakStatusRepository)

                    kelvinSakService.hentSakStatus(personIdenter)
                }
            tellKildesystem(kelvinSaker, null, "/kelvin/sakerByFnr")
            respond(kelvinSaker.map {
                SakStatusOverlappskontroll(
                    sakId = it.sakId,
                    statusKode = it.statusKode,
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
                info(description = "Sjekker om en person eksisterer i AAP-arena")
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
        route("/arena/person/aap/signifikant-historikk") {
            post<CallIdHeader, SignifikanteSakerResponse, SignifikanteSakerRequest>(
                info(description = "Sjekker om en person kan behandles i Kelvin mtp. AAP-Arena-historikken deres")
            ) { callIdHeader, requestBody ->
                logger.info("Sjekker om personen kan behandles i Kelvin")
                val callId = receiveCall(callIdHeader, pipeline)
                pipeline.call.response.headers.append(
                    HttpHeaders.ContentType,
                    ContentType.Application.Json.withCharset(Charsets.UTF_8).toString()
                )

                val harSignifikantAAPArenaHistorikk = arenaService.harSignifikantAAPArenaHistorikk(
                    callId,
                    requestBody.personidentifikatorer,
                    requestBody.virkningstidspunkt
                )
                respond(harSignifikantAAPArenaHistorikk)
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

                val arenaSaker = arenaService.hentVedtakUtenUtbetaling(callId, body)

                tellKildesystem(kelvinSaker, arenaSaker, "/maksimumUtenUtbetaling")

                respond(Medium(arenaSaker + kelvinSaker))
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
                val callId = receiveCall(callIdHeader, pipeline)

                val body = requestBody.tilKontrakt()
                sjekkTilgangTilPerson(requestBody.personidentifikator, token())

                val kelvinSaker: List<Vedtak> = dataSource.transaction { connection ->
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
        route("/kelvin/dsop") {
            dsopRoutes(dataSource, pdlGateway, clock)
        }
    }
}

private fun azpForTokenGenHvisIkkeProd(): List<UUID> =
    if (!Miljø.erProd()) listOf(UUID.fromString(requiredConfigForKey("AZP_TOKEN_GEN"))) else emptyList()

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

fun sjekkTilgangTilPerson(personIdent: String, token: OidcToken) {
    if (!token.isClientCredentials()) {
        Metrics.tokentype("m2m")
        val tilgang = TilgangGateway.harTilgangTilPerson(personIdent, token)
        if (!tilgang) {
            throw IngenTilgangException("Har ikke tilgang til person")
        }
    } else {
        Metrics.tokentype("obo")
    }
}


private fun hentAllePersonidenter(
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

val Periode.somDTO: PeriodeDTO get() = PeriodeDTO(fom, tom)
