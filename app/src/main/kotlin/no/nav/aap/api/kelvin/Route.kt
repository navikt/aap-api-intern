package no.nav.aap.api.kelvin

import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.micrometer.core.instrument.DistributionSummary
import java.time.LocalDate
import javax.sql.DataSource
import no.nav.aap.api.Metrics.prometheus
import no.nav.aap.api.intern.behandlingsflyt.SakStatusKelvin
import no.nav.aap.api.kafka.KafkaProducer
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.api.postgres.MeldekortDetaljerRepository
import no.nav.aap.api.postgres.MeldekortPerioderRepository
import no.nav.aap.api.postgres.SakStatusRepository
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DatadelingDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DetaljertMeldekortDTO
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("App")

fun NormalOpenAPIRoute.dataInsertion(
    dataSource: DataSource,
    modiaKafkaProducer: KafkaProducer,
) {
    val antallMeldekortMottattPerRequestHistogram = DistributionSummary.builder("aap_api_intern_insert_meldekort_detaljer_antall_mottatt")
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
                    "Legg inn meldekortperioder for en person. Endepunktet kan kun brukes av behandlingsflyt"
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
                    "Legg inn sakstatus for en person. Endepunktet kan kun brukes av behandlingsflyt"
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
                    "Legg inn sak, behandling, og vedtaksdata for en person. Endepunktet kan kun brukes av behandlingsflyt"
                )
            ).toTypedArray(),
        ) { _, body ->
            val nyttVedtak = dataSource.transaction { connection ->
                val behandlingsRepository = BehandlingsRepository(connection)
                val nyttVedtak = behandlingsRepository.hentVedtaksData(
                    body.sak.fnr.first(),
                    no.nav.aap.komponenter.type.Periode(
                        LocalDate.now().minusYears(100),
                        LocalDate.now().plusYears(1000))
                ).isEmpty()
                behandlingsRepository.lagreBehandling(body.tilDomene(nyttVedtak))
                nyttVedtak
            }

            try {
                modiaKafkaProducer.produce(body.sak.fnr.first(), nyttVedtak)
            } catch (e: Exception) {
                logger.error("Klarte ikke sende melding til kafka", e)
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
                    "Legg inn meldekort-liste for en person. Endepunktet kan kun brukes av behandlingsflyt"
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
                info("Oppdaterer identer for en sak. Endepunktet kan kun brukes av behandlingsflyt")
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