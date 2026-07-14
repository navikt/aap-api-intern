package no.nav.aap.api.actuator

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import javax.sql.DataSource
import no.nav.aap.motor.Motor

fun Routing.actuator(prometheus: PrometheusMeterRegistry, motor: Motor, datasource: DataSource) {
    route("/actuator") {
        get("/metrics") {
            call.respondText(prometheus.scrape())
        }
        get("/live") {
            call.respond(HttpStatusCode.OK, "api")
        }
        get("/ready") {
            val databaseOk = databaseErOppe(datasource)
            val motorOk = motor.kjører()

            when {
                !motorOk -> call.respond(HttpStatusCode.ServiceUnavailable, "Motor kjører ikke")
                !databaseOk -> call.respond(HttpStatusCode.ServiceUnavailable, "Database ikke tilgjengelig")
                else -> call.respond(HttpStatusCode.OK, "Oppe!")
            }
        }
    }
}

private fun databaseErOppe(datasource: DataSource): Boolean =
    try {
        datasource.connection.use { it.isValid(1) }
    } catch (e: Exception) {
        false
    }
