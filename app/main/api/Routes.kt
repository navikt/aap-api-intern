package api

import api.arena.ArenaoppslagRestClient
import api.perioder.PerioderRequest
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.slf4j.LoggerFactory
import java.util.*

private val sikkerLogg = LoggerFactory.getLogger("secureLog")

fun Routing.api(arena: ArenaoppslagRestClient) {
    authenticate {
        post("/perioder") {
            val body = call.receive<PerioderRequest>()
            val callId = UUID.randomUUID()
            call.respond(arena.hentPerioder(callId, body))
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
