package api.afp

import api.arena.ArenaoppslagRestClient
import api.auth.MASKINPORTEN_AFP_OFFENTLIG
import api.auth.MASKINPORTEN_AFP_PRIVAT
import api.sporingslogg.SporingsloggKafkaClient
import api.util.Config
import api.auth.hentConsumerId
import io.ktor.server.application.*
import io.ktor.server.auth.*

import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusMeterRegistry

fun Route.afp(
    config: Config,
    arenaoppslagRestClient: ArenaoppslagRestClient,
    sporingsloggClient: SporingsloggKafkaClient,
    prometheus: PrometheusMeterRegistry
) {
    route("/afp") {
        authenticate(MASKINPORTEN_AFP_PRIVAT) {
            post("/fellesordningen") {
                Afp.doWorkFellesOrdningen(call, config, arenaoppslagRestClient, sporingsloggClient, prometheus)
            }
        }

        authenticate(MASKINPORTEN_AFP_OFFENTLIG) {
            post("/offentlig") {
                Afp.doWorkOffentlig(
                    call,
                    config,
                    arenaoppslagRestClient,
                    sporingsloggClient,
                    prometheus,
                    orgnr = call.hentConsumerId()
                )
            }
        }
    }

    authenticate(MASKINPORTEN_AFP_PRIVAT) {
        post("/fellesordning-for-afp") {
            Afp.doWorkFellesOrdningen(call, config, arenaoppslagRestClient, sporingsloggClient, prometheus)
        }
    }
}
