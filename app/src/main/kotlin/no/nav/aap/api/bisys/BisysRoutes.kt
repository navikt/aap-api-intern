package no.nav.aap.api.bisys

import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import com.papsign.ktor.openapigen.route.tags
import no.nav.aap.api.CallIdHeader
import no.nav.aap.api.Tag
import no.nav.aap.api.azpForTokenGenHvisIkkeProd
import no.nav.aap.api.intern.BisysBarnetilleggRequest
import no.nav.aap.api.intern.BisysBarnetilleggResponse
import no.nav.aap.api.kelvin.BarnetilleggService
import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost
import java.time.Clock
import java.util.*
import javax.sql.DataSource

fun NormalOpenAPIRoute.bisysRoutes(
    dataSource: DataSource,
    pdlGateway: IPdlGateway,
    clock: Clock = Clock.systemDefaultZone(),
) {
    tag(Tag.Bisys) {
        route("/bisys/barnetillegg").authorizedPost<CallIdHeader, BisysBarnetilleggResponse, BisysBarnetilleggRequest>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SE,
                authorizedAzps = listOf(
                    UUID.fromString(requiredConfigForKey("AZP_BISYS")),
                ) + azpForTokenGenHvisIkkeProd(),
            ),
            null,
            null,
            null,
            info(description = "Endepunkt ment for Bisys. Henter barn med barnetillegg og tilhørende perioder/beløp for en person. Hvis person ikke finnes i Kelvin returneres tom liste."),
            tags(Tag.Bisys),
        ) { _, requestBody ->
            val responseBody = dataSource.transaction { connection ->
                BarnetilleggService(connection, pdlGateway, clock).hentBarnetillegg(
                    requestBody.personidentifikator,
                )
            }

            respond(responseBody)
        }
    }
}
