
package api.util

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum

class Fakes:AutoCloseable {
    val azure = embeddedServer(Netty, port = 0, module = Application::azure)
    val arena = embeddedServer(Netty, port = 0, module = Application::arena)

    override fun close() {
        azure.stop(0L, 0L) //To change body of created functions use File | Settings | File Templates.
        arena.stop(0L, 0L) //To change body of created functions use File | Settings | File Templates.
    }

    init {
        azure.start()
        arena.start()

        System.setProperty("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT", "http://localhost:${azure.port()}")
        System.setProperty("AZURE_APP_CLIENT_ID", "test")
        System.setProperty("AZURE_APP_CLIENT_SECRET", "test")
        System.setProperty("AZURE_OPENID_CONFIG_JWKS_URI", "http://localhost:${azure.port()}/jwks")
        System.setProperty("AZURE_OPENID_CONFIG_ISSUER", "test")
        System.setProperty("KELVIN_PROXY_BASE_URL", "http://localhost:${azure.port()}")
        System.setProperty("ARENAOPPSLAG_PROXY_BASE_URL", "http://localhost:${arena.port()}")
        System.setProperty("ARENAOPPSLAG_SCOPE", "test")
    }
}

data class Token(val expires_in: Long, val access_token: String)

fun Application.azure() {
    install(ContentNegotiation) { jackson() }
    routing {
        get("/jwks") {
            println("Received request for jwks")
            call.respond(HttpStatusCode.OK, AZURE_JWKS)
        }
        post("/jwt") {
            println("Received request for jwt")
            call.respond(HttpStatusCode.OK, Token(3600, "test"))
        }
    }
}

fun Application.arena(){
    install(ContentNegotiation) { jackson() }
    routing {
        post("/intern/maksimum") {
            println("Received request for maksimum")
            call.respond(
                Maksimum(
                    vedtak = emptyList()
                )
            )
        }
    }
}

fun EmbeddedServer<*, *>.port(): Int =
    runBlocking { this@port.engine.resolvedConnectors() }
        .first { it.type == ConnectorType.HTTP }
        .port
