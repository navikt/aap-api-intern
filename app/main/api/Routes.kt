package api

import api.arena.IArenaoppslagRestClient
import api.maksimum.*
import api.perioder.PerioderInkludert11_17Response
import api.perioder.PerioderResponse
import api.postgres.BehandlingsRepository
import api.postgres.MeldekortPerioderRepository
import api.postgres.SakStatusRepository
import api.postgres.TilkjentDB
import api.util.perioderMedAAp
import com.papsign.ktor.openapigen.APITag
import com.papsign.ktor.openapigen.annotations.parameters.HeaderParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.path.normal.route
import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.api.intern.Kilde
import no.nav.aap.api.intern.PersonEksistererIAAPArena
import no.nav.aap.api.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.auth.audience
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import javax.sql.DataSource

private val logger = LoggerFactory.getLogger("App")

data class CallIdHeader(
    @HeaderParam("callId") val `Nav-CallId`: String? = null,
    @HeaderParam("correlation id") val `X-Correlation-Id`: String? = null,
) {
    fun callId(): UUID? {
        val callId = listOfNotNull(`Nav-CallId`, `X-Correlation-Id`).firstOrNull()

        return callId?.let { UUID.fromString(it) }
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
    httpCallCounter: PrometheusMeterRegistry
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
                val callId = callIdHeader.callId() ?: UUID.randomUUID().also {
                    logger.info("CallID ble ikke gitt på kall mot: /perioder")
                }
                val kelvinPerioder = dataSource.transaction { connection ->
                    val behandlingsRepository = BehandlingsRepository(connection)
                    val vedtaksdata = behandlingsRepository.hentVedtaksData(requestBody.personidentifikator)
                    perioderMedAAp(vedtaksdata, Periode(requestBody.fraOgMedDato, requestBody.tilOgMedDato))
                }
                respond(PerioderResponse(arena.hentPerioder(callId, requestBody).perioder+kelvinPerioder))
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
                val callId = callIdHeader.callId() ?: UUID.randomUUID().also {
                    logger.info("CallID ble ikke gitt på kall mot: /perioder/aktivitetfase")
                }

                respond(arena.hentPerioderInkludert11_17(callId, requestBody))
            }
            route("/meldekort").post<CallIdHeader, List<KelvinPeriode>, InternVedtakRequest>(
                info(description = "Henter meldekort perioder for en person innen gitte datointerval")
            ) { callIdHeader, requestBody ->
                val perioder = dataSource.transaction { connection ->
                    val meldekortPerioderRepository = MeldekortPerioderRepository(connection)
                    meldekortPerioderRepository.hentMeldekortPerioder(requestBody.personidentifikator)
                }.filter { it.tom > requestBody.fraOgMedDato && it.fom < requestBody.tilOgMedDato }
                respond(perioder, HttpStatusCode.OK)
            }
        }
    }

    tag(Tag.Saker) {
        route("/sakerByFnr").post<CallIdHeader, List<SakStatus>, SakerRequest>(
            info(description = "Henter saker for en person")
        ) { callIdHeader, requestBody ->
            httpCallCounter.httpCallCounter(
                "/sakerByFnr",
                pipeline.call.audience(),
                azpName() ?: ""
            ).increment()

            val callId = callIdHeader.callId() ?: UUID.randomUUID().also {
                logger.info("CallID ble ikke gitt på kall mot: /sakerByFnr")
            }

            val kelvinSaker: List<SakStatus> = dataSource.transaction { connection ->
                val sakStatusRepository = SakStatusRepository(connection)
                requestBody.personidentifikatorer.flatMap {
                    sakStatusRepository.hentSakStatus(it)
                }
            }

            respond(arena.hentSakerByFnr(callId, requestBody) + kelvinSaker)
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
                val callId = callIdHeader.callId() ?: UUID.randomUUID().also {
                    logger.info("CallID ble ikke gitt på kall mot: /person/aap/eksisterer")
                }

                respond(PersonEksistererIAAPArena(arena.hentPersonEksistererIAapContext(callId, requestBody).eksisterer))
            }
        }
    }

    tag(Tag.Maksimum) {
        route("/maksimumUtenUtbetaling") {
            post<CallIdHeader, api.maksimum.Medium, InternVedtakRequest>(
                info(description = "Henter maksimumsløsning uten utbetalinger for en person innen gitte datointerval")
            ) { callIdHeader, requestBody ->
                httpCallCounter.httpCallCounter(
                    "/maksimumUtenUtbetaling",
                    pipeline.call.audience(),
                    azpName() ?: ""
                ).increment()
                val callId = callIdHeader.callId() ?: UUID.randomUUID().also {
                    logger.info("CallID ble ikke gitt på kall mot: /maksimum")
                }

                val kelvinSaker: List<VedtakUtenUtbetaling> = dataSource.transaction{ connection ->
                    val behandlingsRepository = BehandlingsRepository(connection)
                    hentMedium(requestBody.personidentifikator, Periode(requestBody.fraOgMedDato,requestBody.tilOgMedDato), behandlingsRepository).vedtak
                }
                respond(
                    Medium(
                        arena.hentMaksimum(callId, requestBody).vedtak.map { it.fraKontraktUtenUtbetaling() } + kelvinSaker
                    )
                )
            }
        }
        route("/maksimum") {
            post<CallIdHeader, api.maksimum.Maksimum, InternVedtakRequest>(
                info(description = "Henter maksimumsløsning for en person innen gitte datointerval")
            ) { callIdHeader, requestBody ->
                httpCallCounter.httpCallCounter(
                    "/maksimum",
                    pipeline.call.audience(),
                    azpName() ?: ""
                ).increment()
                val callId = callIdHeader.callId() ?: UUID.randomUUID().also {
                    logger.info("CallID ble ikke gitt på kall mot: /maksimum")
                }

                val kelvinSaker: List<Vedtak> = dataSource.transaction{ connection ->
                    val behandlingsRepository = BehandlingsRepository(connection)
                    behandlingsRepository.hentMaksimum(requestBody.personidentifikator, Periode(requestBody.fraOgMedDato,requestBody.tilOgMedDato)).vedtak
                }
                respond(
                    Maksimum(
                        arena.hentMaksimum(callId, requestBody).fraKontrakt().vedtak + kelvinSaker
                    )
                )
            }
        }
    }
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

fun hentMedium(fnr: String, interval: Periode, behandlingsRepository: BehandlingsRepository): Medium {
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
                        vedtaksId = behandling.behandlingsReferanse,
                        dagsats = right?.verdi?.dagsats?: 0,
                        status =
                            if (behandling.behandlingStatus == no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.IVERKSETTES || periode.tom.isAfter(
                                    LocalDate.now()
                                )
                            ) {
                                Status.LØPENDE.toString()
                            } else if (behandling.behandlingStatus == no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.AVSLUTTET) {
                                Status.AVSLUTTET.toString()
                            } else {
                                Status.UTREDES.toString()
                            },
                        saksnummer = behandling.sak.saksnummer,
                        vedtaksdato = behandling.vedtaksDato.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        vedtaksTypeKode = "",
                        vedtaksTypeNavn = "",
                        rettighetsType = left.verdi ?: "",
                        beregningsgrunnlag = right?.verdi?.grunnlag?.toInt()?:0,
                        barnMedStonad = right?.verdi?.antallBarn?:0,
                        kildesystem = Kilde.KELVIN.toString(),
                        samordningsId = null,
                        opphorsAarsak = null
                    )
                )
            }
        ).komprimer()
            .map { it.verdi.tilVedtakUtenUtbetaling(no.nav.aap.api.intern.Periode(it.periode.fom, it.periode.tom)) }
            .filter { (it.status == Status.LØPENDE.toString() || it.status == Status.AVSLUTTET.toString())}
    }

    return Medium(vedtak)
}