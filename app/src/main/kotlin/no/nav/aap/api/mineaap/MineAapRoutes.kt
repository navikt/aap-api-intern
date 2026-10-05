package no.nav.aap.api.mineaap

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.api.hentAllePersonidenter
import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.api.util.auth.authorizedClientIds
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.server.auth.personBruker
import javax.sql.DataSource

fun NormalOpenAPIRoute.mineAaapApi(
    dataSource: DataSource,
    pdlGateway: IPdlGateway,
) {
    route("/mine-aap") {
        authorizedClientIds(
            requiredConfigForKey("MINE_AAP_CLIENT_ID")
        )
        route("/saker-med-behandlinger") {
            get<Unit, List<Unit>> {
                val personIdenter = hentAllePersonidenter(listOf(personBruker().pid), pdlGateway)
                respond(listOf())
            }
        }

    }
}
