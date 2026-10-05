package no.nav.aap.api.util.auth

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.isHandled
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import org.slf4j.LoggerFactory


private val log = LoggerFactory.getLogger("ClientIdAllowlist")
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
            if (call.isHandled) return@on                  // innloggingen har allerede svart
            val principal = call.principal<JWTPrincipal>()
            if (principal == null) {
                call.respond(HttpStatusCode.Unauthorized)  // ikke innlogget → 401
                return@on
            }
            val clientId = principal.getClaim("client_id", String::class)
            if (clientId == null || clientId !in config.authorizedClientIds) {
                log.warn("client_id {} har ikke tilgang til {}", clientId, call.request.path())
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
