package no.nav.aap.api.eien

import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
import com.papsign.ktor.openapigen.route.tags
import io.ktor.http.HttpStatusCode
import java.util.UUID
import javax.sql.DataSource
import no.nav.aap.api.CallIdHeader
import no.nav.aap.api.Tag
import no.nav.aap.api.azpForTokenGenHvisIkkeProd
import no.nav.aap.api.hentAllePersonidenter
import no.nav.aap.api.intern.EienSakerRequest
import no.nav.aap.api.intern.EienSakerResponse
import no.nav.aap.api.kelvin.KelvinSakService
import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.api.postgres.SakStatusRepository
import no.nav.aap.api.tellKildesystem
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.exception.ApiException
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.server.auth.token
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost

fun NormalOpenAPIRoute.eienRoutes(
    dataSource: DataSource,
    pdlGateway: IPdlGateway,
) {
    tag(Tag.Eien) {
        route("/eien/sakerByFnr").authorizedPost<CallIdHeader, EienSakerResponse, EienSakerRequest>(
            AuthorizationBodyPathConfig(
                operasjon = Operasjon.SE,
                authorizedAzps = listOf(
                    UUID.fromString(requiredConfigForKey("AZP_EIEN")),
                ) + azpForTokenGenHvisIkkeProd(),
            ),
            null,
            null,
            null,
            info(
                description = """
                    Endepunkt ment for prosjekt Eien (Én innbygger - ett Nav). Krever on-behalf-of-token
                    for veileder. Veileders tilgang til personen kontrolleres av tilgang-plugin.
                    Returnerer kun saker fra Kelvin, ikke Arena. En tom liste betyr derfor ikke
                    at personen ikke har AAP. Ikke tilgjengelig i prod ennå.
                """.trimIndent()
            ),
            tags(Tag.Eien),
        ) { _, requestBody ->
            if (token().isClientCredentials()) {
                throw IkkeTillattException("Endepunktet krever on-behalf-of-token.")
            }

            if (Miljø.erProd()) {
                throw ApiException(
                    status = HttpStatusCode.NotImplemented,
                    message = "Endepunktet er ikke tilgjengelig i prod ennå",
                )
            }

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

            tellKildesystem(kelvinSaker, null, "/eien/sakerByFnr")

            respond(EienSakerResponse(saker = kelvinSaker.map { it.tilEienSak() }))
        }
    }
}
