package no.nav.aap.api

import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.path
import io.ktor.server.routing.RoutingCall
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.server.auth.audience

object Metrics {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    fun httpRequestTeller(call: RoutingCall) {
        val path = call.request.path()
        val audience = call.audience()
        val azpName = call.principal<JWTPrincipal>()?.let {
            it.payload.claims["azp_name"]?.asString()
        } ?: ""
        prometheus.counter(
            "http_call",
            listOf(Tag.of("path", path), Tag.of("audience", audience), Tag.of("azp_name", azpName))
        ).increment()
    }

    fun kildesystemTeller(kildesystem: String, path: String): Counter =
        prometheus.counter(
            "api_intern_kildesystem", listOf(Tag.of("kildesystem", kildesystem), Tag.of("path", path))
        )
}
