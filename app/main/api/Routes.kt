package api

import api.arena.ArenaoppslagRestClient
import api.perioder.PerioderRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.slf4j.LoggerFactory
import java.util.*

private val sikkerLogg = LoggerFactory.getLogger("secureLog")
private val logger = LoggerFactory.getLogger("App")

private val NAV_CALL_ID_HEADER_NAMES =
    listOf(
        "Nav-CallId",
        "Nav-Callid",
        "X-Correlation-Id",
    )

private fun resolveCallId(call: ApplicationCall): String {
    return NAV_CALL_ID_HEADER_NAMES
        .mapNotNull { call.request.header(it) }
        .firstOrNull { it.isNotEmpty() }
        ?: UUID.randomUUID().toString()
}

fun Routing.api(arena: ArenaoppslagRestClient) {
    authenticate {
        route("/perioder") {
            post {
                val body = call.receive<PerioderRequest>()
                val callId = UUID.fromString(resolveCallId(call))
                call.respond(arena.hentPerioder(callId, body))
            }
            post("/aktivitetfase") {
                val body = call.receive<PerioderRequest>()
                val callId = UUID.fromString(resolveCallId(call))
                call.respond(arena.hentPerioderInkludert11_17(callId, body))
            }
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
