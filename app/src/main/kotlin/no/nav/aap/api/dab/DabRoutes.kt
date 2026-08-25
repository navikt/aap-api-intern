package no.nav.aap.api.dab

import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import com.papsign.ktor.openapigen.route.tags
import java.util.UUID
import javax.sql.DataSource
import no.nav.aap.api.CallIdHeader
import no.nav.aap.api.Tag
import no.nav.aap.api.azpForTokenGenHvisIkkeProd
import no.nav.aap.api.hentAllePersonidenter
import no.nav.aap.api.intern.DabSakerRequest
import no.nav.aap.api.intern.DabSakerResponse
import no.nav.aap.api.kelvin.KelvinSakService
import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.api.postgres.SakStatusRepository
import no.nav.aap.api.receiveCall
import no.nav.aap.api.arena.ArenaService
import no.nav.aap.api.tellKildesystem
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost

fun NormalOpenAPIRoute.dabRoutes(
    dataSource: DataSource,
    arenaService: ArenaService,
    pdlGateway: IPdlGateway,
) {
    tag(Tag.DAB) {
        route("/dab/sakerByFnr").authorizedPost<CallIdHeader, DabSakerResponse, DabSakerRequest>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SE,
                authorizedAzps = listOf(
                    UUID.fromString(requiredConfigForKey("AZP_DAB")),
                ) + azpForTokenGenHvisIkkeProd(),
            ),
            null,
            null,
            null,
            info(description = "Endepunkt ment for DAB. Henter saker for en person, med et redusert sett med felter."),
            tags(Tag.DAB),
        ) { callIdHeader, requestBody ->
            val callId = receiveCall(callIdHeader)
            val personidenter = hentAllePersonidenter(
                listOf(requestBody.personidentifikator),
                pdlGateway,
            )

            val kelvinSaker = dataSource.transaction { connection ->
                KelvinSakService(
                    SakStatusRepository(connection),
                    BehandlingsRepository(connection),
                ).hentSakStatus(personidenter)
            }
            val arenaSaker = arenaService.hentSaker(callId, personidenter)

            tellKildesystem(kelvinSaker, arenaSaker, "/dab/sakerByFnr")

            respond(DabSakerResponse(saker = kelvinSaker.kelvinTilDabSaker() + arenaSaker.arenaTilDabSaker()))
        }
    }
}
