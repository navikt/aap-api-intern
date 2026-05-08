package no.nav.aap.api.kelvin

import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import io.ktor.server.response.*
import io.micrometer.core.instrument.DistributionSummary
import no.nav.aap.api.Metrics.prometheus
import no.nav.aap.api.intern.behandlingsflyt.OppdaterIdenterDto
import no.nav.aap.api.intern.behandlingsflyt.SakStatusKelvin
import no.nav.aap.api.motor.jobber.AapHendelsePayload
import no.nav.aap.api.motor.jobber.Hendelse
import no.nav.aap.api.motor.jobber.ModiaHendelsePayload
import no.nav.aap.api.motor.jobber.SendAapHendelseUtfører
import no.nav.aap.api.motor.jobber.SendModiaHendelseUtfører
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.api.postgres.MeldekortDetaljerRepository
import no.nav.aap.api.postgres.MeldekortPerioderRepository
import no.nav.aap.api.postgres.SakStatusRepository
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DatadelingDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DetaljertMeldekortDTO
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val logger = LoggerFactory.getLogger("App")

fun NormalOpenAPIRoute.dataInsertion(
    dataSource: DataSource,
) {
    val antallMeldekortMottattPerRequestHistogram =
        DistributionSummary.builder("aap_api_intern_insert_meldekort_detaljer_antall_mottatt")
            .publishPercentileHistogram(true)
            .register(prometheus)

    route("/api/insert") {
        route("/meldeperioder").authorizedPost<Unit, Unit, MeldekortPerioderDTO>(
            routeConfig = AuthorizationBodyPathConfig(
                operasjon = Operasjon.SE,
                applicationsOnly = true,
                applicationRole = "add-data",
            ),
            modules = listOf(
                info(
                    "Legg inn meldekortperioder",
                    "Legg inn meldekortperioder for en person. Endepunktet kan kun brukes av behandlingsflyt. Kalles ved hvert stopp i behandlingen før vedtak."
                )
            ).toTypedArray(),
        ) { _, body ->
            dataSource.transaction { connection ->
                val meldekortPerioderRepository = MeldekortPerioderRepository(connection)
                meldekortPerioderRepository.lagreMeldekortPerioder(
                    body.personIdent,
                    body.meldekortPerioder
                )
            }
            pipeline.call.respond(HttpStatusCode.OK)
        }
        route("/sakStatus").authorizedPost<Unit, Unit, SakStatusKelvin>(
            routeConfig = AuthorizationBodyPathConfig(
                operasjon = Operasjon.SE,
                applicationsOnly = true,
                applicationRole = "add-data",
            ),
            modules = listOf(
                info(
                    "Legg inn sakstatus for en person. Endepunktet kan kun brukes av behandlingsflyt. Kalles ved hvert stopp i behandlingen før vedtak."
                )
            ).toTypedArray(),
        ) { _, body ->
            dataSource.transaction { connection ->
                val sakStatusRepository = SakStatusRepository(connection)
                sakStatusRepository.lagreSakStatusFraKelvin(body.ident, body.status)
            }
            pipeline.call.respond(HttpStatusCode.OK)
        }
        route("/vedtak").authorizedPost<Unit, Unit, DatadelingDTO>(
            routeConfig = AuthorizationBodyPathConfig(
                operasjon = Operasjon.SE,
                applicationsOnly = true,
                applicationRole = "add-data",
            ),
            modules = listOf(
                info(
                    "Legg inn sak, behandling, og vedtaksdata",
                    "Legg inn sak, behandling, og vedtaksdata for en person. Endepunktet kan kun brukes av behandlingsflyt. Kalles etter at vedtak er fattet, men før behandlingen er avsluttet."
                )
            ).toTypedArray(),
        ) { _, body ->
            dataSource.transaction { connection ->
                val fnr = body.sak.fnr.first()
                val behandlingsRepository = BehandlingsRepository(connection)
                val nyttVedtak = behandlingsRepository.erNyttVedtak(fnr)
                behandlingsRepository.lagreBehandling(body.sak.fnr, body.tilDomene(nyttVedtak))

                val hendelse = if (nyttVedtak) Hendelse.FORSTEGANGSVEDTAK else Hendelse.ENDRINGSVEDTAK

                val jobb = JobbInput(SendAapHendelseUtfører)
                    .medPayload(DefaultJsonMapper.toJson(AapHendelsePayload(fnr, hendelse)))
                val modiaJobb = JobbInput(SendModiaHendelseUtfører)
                    .medPayload(DefaultJsonMapper.toJson(ModiaHendelsePayload(fnr, nyttVedtak)))
                val repo = FlytJobbRepository(connection)
                repo.leggTil(jobb)
                repo.leggTil(modiaJobb)
            }

            pipeline.call.respond(HttpStatusCode.OK)
        }
        route("/meldekort-detaljer").authorizedPost<Unit, Unit, List<DetaljertMeldekortDTO>>(
            routeConfig = AuthorizationBodyPathConfig(
                operasjon = Operasjon.SE,
                applicationsOnly = true,
                applicationRole = "add-data",
            ),
            modules = listOf(
                info(
                    "Legg inn detaljerte meldekort",
                    "Legg inn meldekort-liste for en person. Endepunktet kan kun brukes av behandlingsflyt. Kalles ved hvert stopp i behandlingen, også før vedtak."
                )
            ).toTypedArray()
        ) { _, meldekortPåSammeSak: List<DetaljertMeldekortDTO> ->
            dataSource.transaction { connection ->
                val meldekortPerioderRepository = MeldekortDetaljerRepository(connection)
                val domeneKort = meldekortPåSammeSak.map { it.tilDomene() }
                meldekortPerioderRepository.lagre(domeneKort)
            }

            antallMeldekortMottattPerRequestHistogram.record(meldekortPåSammeSak.size.toDouble())

            pipeline.call.respond(HttpStatusCode.OK)
        }

        route("/oppdater-identer").authorizedPost<Unit, Unit, OppdaterIdenterDto>(
            routeConfig = AuthorizationBodyPathConfig(
                operasjon = Operasjon.SE,
                applicationsOnly = true,
                applicationRole = "add-data",
            ),
            modules = listOf(
                info("Oppdaterer identer for en sak. Endepunktet kan kun brukes av behandlingsflyt. Kalles manuelt fra Paw Patrol.")
            ).toTypedArray()
        ) { _, req ->
            dataSource.transaction { connection ->
                val behandlingsRepository = BehandlingsRepository(connection)
                behandlingsRepository.lagreOppdaterteIdenter(
                    saksnummer = req.saksnummer,
                    identer = req.identer
                )
            }

            respondWithStatus(HttpStatusCode.OK)
        }
    }
}