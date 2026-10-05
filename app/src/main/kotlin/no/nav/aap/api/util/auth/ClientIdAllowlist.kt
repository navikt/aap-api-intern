package no.nav.aap.api.util.auth

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException

/**
 * Sjekker claim
 * `client_id`, en streng på formen `<cluster>:<namespace>:<appnavn>`
 * (se docs.nais.io/auth/tokenx/reference).
 */
data class ClientIdAllowlistConfig(val authorizedClientIds: List<String>)

/**
 * Installerer en route-scoped plugin som avviser forespørsler der `client_id`-claimet
 * i det autentiserte JWT-et ikke finnes i [config]s allowlist.
 */
fun Route.installClientIdAllowlist(config: ClientIdAllowlistConfig) {
    install(createRouteScopedPlugin("ClientIdAllowlistPlugin") {
        on(AuthenticationChecked) { call ->
            val clientId = call.principal<JWTPrincipal>()?.getClaim("client_id", String::class)
            if (clientId == null || clientId !in config.authorizedClientIds) {
                val feil = IkkeTillattException("Ingen tilgang, client_id ikke godkjent")
                call.respond(feil.status, feil.tilApiErrorResponse())
            }
        }
    })
}

/**
 * Bekvemmelighetsfunksjon for ktor-openapigen-ruter: installerer [installClientIdAllowlist]
 * på den underliggende [Route] til denne [NormalOpenAPIRoute].
 */
fun NormalOpenAPIRoute.authorizedClientIds(vararg clientId: String) =
    ktorRoute.installClientIdAllowlist(ClientIdAllowlistConfig(clientId.toList()))
