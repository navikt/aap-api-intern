package api.kelvin

import api.Tag
import api.maksimum.VedtakDataKelvin
import api.postgres.MeldekortPerioderRepository
import api.postgres.VedtaksDataRepository
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.tag
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import io.ktor.server.response.*
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.authorizedPost
import tilgang.Operasjon
import javax.sql.DataSource
fun NormalOpenAPIRoute.dataInsertion(dataSource: DataSource) {
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
                meldekortPerioderRepository.lagreMeldekortPerioder(body.personIdent, body.meldekortPerioder)
            }
            pipeline.call.respond(HttpStatusCode.OK)
        }

        route("/vedtak").authorizedPost<Unit, Unit, VedtakDataKelvin>(
            routeConfig = AuthorizationBodyPathConfig(
                operasjon = Operasjon.SE,
                applicationsOnly = true,
                applicationRole = "add-data",
            ),
            modules = listOf(
                info(
                    "Legg inn vedtaksData",
                    "Legg inn vedtaksdata for en person. Endepunktet kan kun brukes av behandlingsflyt"
                )
            ).toTypedArray(),
        ){
            _, body ->
            dataSource.transaction { connection ->
                val vedtakRepository = VedtaksDataRepository(connection)
                vedtakRepository.lagre(body)
            }
            pipeline.call.respond(HttpStatusCode.OK)
        }
    }
}