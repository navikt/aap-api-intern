package no.nav.aap.api.kelvin

import no.nav.aap.api.kafka.KafkaProducer
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.api.postgres.MeldekortDetaljerRepository
import no.nav.aap.api.postgres.MeldekortPerioderRepository
import no.nav.aap.api.postgres.SakStatusRepository
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import io.ktor.server.response.*
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DatadelingDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DetaljertMeldekortDTO
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost
import org.slf4j.LoggerFactory
import java.time.LocalDate
import javax.sql.DataSource

private val logger = LoggerFactory.getLogger("App")

fun NormalOpenAPIRoute.dataInsertion(dataSource: DataSource, modiaKafkaProducer: KafkaProducer) {
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
                sakStatusRepository.lagreSakStatus(body.ident, body.status)
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
                val tidligereVedtak = behandlingsRepository.hentVedtaksData(
                    body.sak.fnr.first(),
                    no.nav.aap.komponenter.type.Periode(
                        LocalDate.now().minusYears(100),
                        LocalDate.now().plusYears(1000))
                ).isEmpty()
                behandlingsRepository.lagreBehandling(body.tilDomene())
                tidligereVedtak
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
        ) { _, kortene: List<DetaljertMeldekortDTO> ->
            dataSource.transaction { connection ->
                val meldekortPerioderRepository = MeldekortDetaljerRepository(connection)
                val domeneKort = kortene.map { it.tilDomene() }

                meldekortPerioderRepository.lagre(domeneKort)

            }

            pipeline.call.respond(HttpStatusCode.OK)
        }

    }
}