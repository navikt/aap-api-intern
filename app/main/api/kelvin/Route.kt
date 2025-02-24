package api.kelvin

import api.Tag
import api.postgres.MeldekortPerioderRepository
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.tag
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import javax.sql.DataSource

fun NormalOpenAPIRoute.dataInsertion(dataSource: DataSource) {
    tag(Tag.Insertion) {
        route("/api/insert") {
            route("meldeperioder").post<Unit, List<Periode>, MeldekortPerioderDTO>(
                info("Legger inn meldekortperioder fra Kelvin")
            ) { _, body ->
                val perioder = dataSource.transaction { connection ->
                    val meldekortPerioderRepository = MeldekortPerioderRepository(connection)
                    meldekortPerioderRepository.lagreMeldekortPerioder(body.personIdent, body.meldekortPerioder)
                }
                respond(perioder,HttpStatusCode.OK)
            }
        }
    }
}