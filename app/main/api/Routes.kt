package api

import api.arena.ArenaoppslagRestClient
import api.perioder.PerioderInkludert11_17Response
import api.perioder.PerioderResponse
import api.perioder.SakStatus
import com.papsign.ktor.openapigen.annotations.parameters.HeaderParam
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import org.slf4j.LoggerFactory
import java.util.*

private val logger = LoggerFactory.getLogger("App")

data class CallIdHeader(
    @HeaderParam("callId") val `Nav-CallId`: String? = null,
    @HeaderParam("correlation id") val `X-Correlation-Id`: String? = null,
) {
    fun callId(): UUID? {
        val callId = listOfNotNull(`Nav-CallId`, `X-Correlation-Id`).firstOrNull()

        return callId?.let { UUID.fromString(it) }
    }
}

fun NormalOpenAPIRoute.api(arena: ArenaoppslagRestClient, httpCallCounter: PrometheusMeterRegistry) {

    route("/perioder") {
        post<InternVedtakRequest, PerioderResponse, CallIdHeader> { requestBody, callIdHeader ->
            httpCallCounter.httpCallCounter("/perioder").increment()
            val callId = callIdHeader.callId() ?: UUID.randomUUID().also {
                logger.info("CallID ble ikke gitt på kall mot: /perioder")
            }

            respond(arena.hentPerioder(callId, requestBody))
        }

        route("/aktivitetfase").post<InternVedtakRequest, PerioderInkludert11_17Response, CallIdHeader> { requestBody, callIdHeader ->
            httpCallCounter.httpCallCounter("/perioder/aktivitetfase").increment()
            val callId = callIdHeader.callId() ?: UUID.randomUUID().also {
                logger.info("CallID ble ikke gitt på kall mot: /perioder/aktivitetfase")
            }

            respond(arena.hentPerioderInkludert11_17(callId, requestBody))
        }
    }

    route("/sakerByFnr").post<SakerRequest, List<SakStatus>, CallIdHeader> { requestBody, callIdHeader ->
        httpCallCounter.httpCallCounter("/sakerByFnr").increment()
        val callId = callIdHeader.callId() ?: UUID.randomUUID().also {
            logger.info("CallID ble ikke gitt på kall mot: /sakerByFnr")
        }

        respond(arena.hentSakerByFnr(callId, requestBody))
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
