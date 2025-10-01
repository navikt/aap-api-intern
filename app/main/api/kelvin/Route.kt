package api.kelvin

import api.kafka.KafkaProducer
import api.kafka.ModiaKafkaProducer
import api.kafka.ModiaRecord
import api.pdl.IPdlClient
import api.pdl.PdlClient
import api.postgres.BehandlingsRepository
import api.postgres.MeldekortDetaljerRepository
import api.postgres.MeldekortPerioderRepository
import api.postgres.SakStatusRepository
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
import javax.sql.DataSource

fun NormalOpenAPIRoute.dataInsertion(dataSource: DataSource, pdlClient: IPdlClient, kafkaProducer: KafkaProducer) {
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
            var meldingstype : ModiaRecord.Meldingstype ?= null
            dataSource.transaction { connection ->
                val behandlingsRepository = BehandlingsRepository(connection)
                meldingstype = behandlingsRepository.lagreBehandling(body.tilDomene())
            }
            try {
                kafkaProducer.produce(body.sak.fnr.first(), meldingstype!!)
            }finally {
                pipeline.call.respond(HttpStatusCode.OK)
            }
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