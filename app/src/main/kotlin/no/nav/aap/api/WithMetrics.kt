package no.nav.aap.api

import io.micrometer.core.instrument.MeterRegistry

interface WithMetrics {
    fun registrerMetrics(registry: MeterRegistry)
}
