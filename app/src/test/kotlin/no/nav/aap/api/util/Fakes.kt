package no.nav.aap.api.util

import no.nav.aap.api.pdl.PdlIdenter
import no.nav.aap.api.pdl.PdlIdenterData
import no.nav.aap.api.util.graphql.GraphQLResponse
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
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.tilgang.TilgangResponse


class Fakes():AutoCloseable {
    val azure = embeddedServer(Netty, port = 0, module = Application::azure).start()
    val arena = embeddedServer(Netty, port = 0, module = Application::arena).start()
    val pdl = embeddedServer(Netty, port = 0, module = Application::pdlFake).start()
    val tilgang = embeddedServer(Netty, port = 0, module = Application::tilgangFake).start()
    val kafka = KafkaFake()

    override fun close() {
        azure.stop(0L, 0L)
        arena.stop(0L, 0L)
        pdl.stop(0L, 0L)
        tilgang.stop(0L, 0L)
        kafka.close()
    }

    init {
        // Azure
        System.setProperty("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT", "http://localhost:${azure.port()}")
        System.setProperty("AZURE_APP_CLIENT_ID", "test")
        System.setProperty("AZURE_APP_CLIENT_SECRET", "test")
        System.setProperty("AZURE_OPENID_CONFIG_JWKS_URI", "http://localhost:${azure.port()}/jwks")
        System.setProperty("AZURE_OPENID_CONFIG_ISSUER", "test")

        // Kelvin
        System.setProperty("KELVIN_PROXY_BASE_URL", "http://localhost:${azure.port()}")

        // Arena
        System.setProperty("ARENAOPPSLAG_PROXY_BASE_URL", "http://localhost:${arena.port()}")
        System.setProperty("ARENAOPPSLAG_SCOPE", "test")

        // PDL
        System.setProperty("INTEGRASJON_PDL_URL", "http://localhost:${pdl.port()}/graphql")
        System.setProperty("INTEGRASJON_PDL_SCOPE", "test")

        // Tilgang
        System.setProperty("INTEGRASJON_TILGANG_URL", "http://localhost:${tilgang.port()}")
        System.setProperty("INTEGRASJON_TILGANG_SCOPE", "scope")
    }
}
data class TestToken(
    val access_token: String,
    val refresh_token: String = "very.secure.token",
    val id_token: String = "very.secure.token",
    val token_type: String = "token-type",
    val scope: String? = null,
    val expires_in: Int = 3599,
)

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
        post {
            val token = OidcToken(AzureTokenGen(
                issuer = "test",
                audience = "test"
            ).generate(isApp = false)).token()
            call.respond(TestToken(access_token = token))}

    }
}

fun Application.arena(){
    install(ContentNegotiation) { jackson() }
    routing {
        post("/intern/maksimum") {
            call.respond(
                Maksimum(
                    vedtak = emptyList()
                )
            )
        }
    }
}

fun Application.pdlFake() {
    install(ContentNegotiation) { jackson() }
    routing {
        post("/graphql") {
            val data = PdlIdenterData(PdlIdenter(emptyList()))
            val response = GraphQLResponse(
                data,
                emptyList()
            )
            call.respond(response)
        }
    }
}

fun Application.tilgangFake() = runBlocking {
    install(ContentNegotiation) {
        jackson()
    }
    routing {
        post("/tilgang/person") {
            call.respond(TilgangResponse(true))
        }
    }
}

fun EmbeddedServer<*, *>.port(): Int =
    runBlocking { this@port.engine.resolvedConnectors() }
        .first { it.type == ConnectorType.HTTP }
        .port
