package api

import api.arena.ArenaoppslagRestClient
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("App")


private val NAV_CALL_ID_HEADER_NAMES =
    listOf(
        "Nav-CallId",
        "X-Correlation-Id",
    )

private fun resolveCallId(call: ApplicationCall): String {
    var callId = NAV_CALL_ID_HEADER_NAMES
        .map { it.lowercase() }
        .mapNotNull { call.request.header(it) }
        .firstOrNull { it.isNotEmpty() }

    if (callId == null) {
        logger.info("CallID ble ikke gitt på kall: $call.request.uri.")
        callId = UUID.randomUUID().toString()
    }
    return callId
}

fun Routing.api(arena: ArenaoppslagRestClient, httpCallCounter: PrometheusMeterRegistry) {
    authenticate {
        route("/perioder") {
            post {
                httpCallCounter.httpCallCounter("/perioder").increment()
                val body = call.receive<InternVedtakRequest>()
                val callId = UUID.fromString(resolveCallId(call))
                call.respond(arena.hentPerioder(callId, body))
            }
            post("/aktivitetfase") {
                httpCallCounter.httpCallCounter("/aktivitetfase").increment()
                val body = call.receive<InternVedtakRequest>()
                val callId = UUID.fromString(resolveCallId(call))
                call.respond(arena.hentPerioderInkludert11_17(callId, body))
            }
        }
        post("/sakerByFnr"){
            httpCallCounter.httpCallCounter("/sakerByFnr").increment()
            val body = call.receive<SakerRequest>()
            val callId = UUID.fromString(resolveCallId(call))
            call.respond(arena.hentSakerByFnr(callId, body))
        }

    }
}

fun Routing.actuator(prometheus: PrometheusMeterRegistry) {
    route("/actuator") {
        get("/metrics") {
            call.respondText(prometheus.scrape())
        }

        get("/live") {
            call.respond(HttpStatusCode.OK, "api")
        }
        get("/ready") {
            call.respond(HttpStatusCode.OK, "api")
        }
    }
}
