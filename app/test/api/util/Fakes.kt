
package api.util

import api.pdl.PdlIdenter
import api.pdl.PdlIdenterData
import api.util.graphql.GraphQLResponse
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.ConnectorType
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum

class Fakes : AutoCloseable {
    val azure = embeddedServer(Netty, port = 0, module = Application::azure)
    val arena = embeddedServer(Netty, port = 0, module = Application::arena)
    val pdl = embeddedServer(Netty, port = 0, module = Application::pdlFake)

    override fun close() {
        azure.stop(0L, 0L) // To change body of created functions use File | Settings | File Templates.
        arena.stop(0L, 0L) // To change body of created functions use File | Settings | File Templates.
        pdl.stop(0L, 0L)
    }

    init {
        azure.start()
        arena.start()
        pdl.start()

        System.setProperty("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT", "http://localhost:${azure.port()}")
        System.setProperty("AZURE_APP_CLIENT_ID", "test")
        System.setProperty("AZURE_APP_CLIENT_SECRET", "test")
        System.setProperty("AZURE_OPENID_CONFIG_JWKS_URI", "http://localhost:${azure.port()}/jwks")
        System.setProperty("AZURE_OPENID_CONFIG_ISSUER", "test")
        System.setProperty("KELVIN_PROXY_BASE_URL", "http://localhost:${azure.port()}")
        System.setProperty("ARENAOPPSLAG_PROXY_BASE_URL", "http://localhost:${arena.port()}")
        System.setProperty("ARENAOPPSLAG_SCOPE", "test")
        System.setProperty("INTEGRASJON_PDL_URL", "http://localhost:${pdl.port()}/graphql")
        System.setProperty("INTEGRASJON_PDL_SCOPE", "test")
    }
}

data class Token(
    val expires_in: Long,
    val access_token: String,
)

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

fun Application.arena() {
    install(ContentNegotiation) { jackson() }
    routing {
        post("/intern/maksimum") {
            println("Received request for maksimum")
            call.respond(
                Maksimum(
                    vedtak = emptyList(),
                ),
            )
        }
    }
}

fun Application.pdlFake() {
    install(ContentNegotiation) { jackson() }
    routing {
        post("/graphql") {
            val data = PdlIdenterData(PdlIdenter(emptyList()))
            val response =
                GraphQLResponse(
                    data,
                    emptyList(),
                )
            call.respond(response)
        }
    }
}

fun EmbeddedServer<*, *>.port(): Int =
    runBlocking { this@port.engine.resolvedConnectors() }
        .first { it.type == ConnectorType.HTTP }
        .port
