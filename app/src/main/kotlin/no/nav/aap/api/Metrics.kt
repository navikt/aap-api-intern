package no.nav.aap.api

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

object Metrics {
    val prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
}
