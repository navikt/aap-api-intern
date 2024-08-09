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

fun Routing.api(arena: ArenaoppslagRestClient) {
    authenticate {
        route("/perioder") {
            post {
                val body = call.receive<PerioderRequest>()
                val callId = call.request.header("Nav-Call-Id")
                val uuidCallid:UUID = UUID.randomUUID()
                try {
                    val uuidCallid = UUID.fromString(callId)
                }catch (e: Exception){
                    logger.warn("Ugyldig callid: $callId")
                }
                call.respond(arena.hentPerioder(uuidCallid, body))
            }
            post("/11-17") {
                val body = call.receive<PerioderRequest>()
                val callId = call.request.header("Nav-Call-Id")
                val uuidCallid:UUID = UUID.randomUUID()
                    try {
                    val uuidCallid = UUID.fromString(callId)
                }catch (e: Exception){
                    logger.warn("Ugyldig callid: $callId")
                }
                call.respond(arena.hentPerioderInkludert11_17(uuidCallid, body))
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
