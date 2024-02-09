package api.util

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheus.PrometheusMeterRegistry

fun PrometheusMeterRegistry.httpCallCounter(consumer: String, path: String): Counter = this.counter(
    "http_call",
    listOf(Tag.of("consumer", consumer), Tag.of("path", path))
)

fun PrometheusMeterRegistry.httpFailedCallCounter(consumer: String, path: String): Counter = this.counter(
    "http_call_failed",
    listOf(Tag.of("consumer", consumer), Tag.of("path", path))
)

fun PrometheusMeterRegistry.sporingsloggFailCounter(consumer: String): Counter = this.counter(
    "sporingslogg_failed",
    listOf(Tag.of("consumer", consumer))
)