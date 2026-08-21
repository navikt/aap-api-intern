package no.nav.aap.api.syfo

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
import no.nav.aap.api.intern.SyfoSakerRequest
import no.nav.aap.api.intern.SyfoSakerResponse
import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.api.receiveCall
import no.nav.aap.api.arena.ArenaService
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost

fun NormalOpenAPIRoute.syfoRoutes(
    dataSource: DataSource,
    arenaService: ArenaService,
    pdlGateway: IPdlGateway,
) {
    tag(Tag.Syfo) {
        route("/syfo/sakerByFnr").authorizedPost<CallIdHeader, SyfoSakerResponse, SyfoSakerRequest>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SE,
                authorizedAzps = listOf(
                    UUID.fromString(requiredConfigForKey("AZP_SYFOPERSON")),
                ) + azpForTokenGenHvisIkkeProd(),
            ),
            null,
            null,
            null,
            info(description = "Henter AAP-saker, søknadsdatoer og vedtak for Modia SYFO."),
            tags(Tag.Syfo),
        ) { callIdHeader, requestBody ->
            val callId = receiveCall(callIdHeader, pipeline)
            val response = SyfoService(
                dataSource = dataSource,
                arenaService = arenaService,
                pdlGateway = pdlGateway,
            ).hentSaker(callId, requestBody.personidentifikator)

            respond(response)
        }
    }
}
