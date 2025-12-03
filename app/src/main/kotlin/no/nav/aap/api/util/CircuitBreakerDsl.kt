package no.nav.aap.api.util

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics
import io.micrometer.core.instrument.MeterRegistry
import java.time.Duration

private val circuitBreakerRegistry: CircuitBreakerRegistry by lazy {
    CircuitBreakerRegistry.ofDefaults()
}

fun registerCircuitBreakerMetrics(meterRegistry: MeterRegistry) {
    TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(circuitBreakerRegistry).bindTo(meterRegistry)
}

class CircuitBreakerDsl {
    var failureRateThreshold: Float = 50f
    var waitDurationInOpenState: Duration = Duration.ofSeconds(30)
    var slowCallDurationThreshold: Duration = Duration.ofMillis(200)
    var permittedNumberOfCallsInHalfOpenState: Int = 10
    var slidingWindowSize: Int = 100
    var minimumNumberOfCalls: Int = 10
    var ignoreExceptions: List<Class<out Throwable>> = emptyList()

    fun build(name: String): CircuitBreaker {
        val config = CircuitBreakerConfig.custom()
            .failureRateThreshold(failureRateThreshold)
            .slowCallDurationThreshold(slowCallDurationThreshold)
            .waitDurationInOpenState(waitDurationInOpenState)
            .permittedNumberOfCallsInHalfOpenState(permittedNumberOfCallsInHalfOpenState)
            .slidingWindowSize(slidingWindowSize)
            .minimumNumberOfCalls(minimumNumberOfCalls)

        if (ignoreExceptions.isNotEmpty()) {
            config.ignoreExceptions(*ignoreExceptions.toTypedArray())
        }

        return circuitBreakerRegistry.circuitBreaker(name, config.build())
    }
}

fun circuitBreaker(name: String, init: CircuitBreakerDsl.() -> Unit = {}): CircuitBreaker {
    val dsl = CircuitBreakerDsl()
    dsl.init()
    return dsl.build(name)
}
