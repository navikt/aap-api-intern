package no.nav.aap.api.util

import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.ConnectorType
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.util.UUID
import kotlinx.coroutines.runBlocking
import no.nav.aap.api.arena.ArenaService
import no.nav.aap.api.pdl.PdlIdenter
import no.nav.aap.api.pdl.PdlIdenterData
import no.nav.aap.api.util.graphql.GraphQLResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerResponse
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import no.nav.aap.oppgave.enhet.EnhetOgOversendelse
import no.nav.aap.oppgave.enhet.PersonRequest
import no.nav.aap.tilgang.TilgangResponse

class Fakes : AutoCloseable {
    val texas = embeddedServer(Netty, port = 0, module = Application::texas).start()
    val arena = embeddedServer(Netty, port = 0, module = Application::arena).start()
    val pdl = embeddedServer(Netty, port = 0, module = Application::pdlFake).start()
    val tilgang = embeddedServer(Netty, port = 0, module = Application::tilgangFake).start()
    val oppgave = embeddedServer(Netty, port = 0, module = Application::oppgaveFake).start()
    val kafka = KafkaFake()
    val arenaService = ArenaService(FakeArenaGateway(), FakeArenaGateway())

    override fun close() {
        texas.stop(0L, 0L)
        arena.stop(0L, 0L)
        oppgave.stop(0L, 0L)
        pdl.stop(0L, 0L)
        tilgang.stop(0L, 0L)
        kafka.close()
    }

    init {
        // Texas
        System.setProperty("nais.token.endpoint", "http://localhost:${texas.port()}/token")
        System.setProperty("nais.token.exchange.endpoint", "http://localhost:${texas.port()}/token/exchange")
        System.setProperty("nais.token.introspection.endpoint", "http://localhost:${texas.port()}/introspect")


        // Arena
        System.setProperty("ARENAOPPSLAG_PROXY_BASE_URL", "http://localhost:${arena.port()}")
        System.setProperty("ARENAOPPSLAG_SCOPE", "test")

        // PDL
        System.setProperty("INTEGRASJON_PDL_URL", "http://localhost:${pdl.port()}/graphql")
        System.setProperty("INTEGRASJON_PDL_SCOPE", "test")

        // Tilgang
        System.setProperty("INTEGRASJON_TILGANG_URL", "http://localhost:${tilgang.port()}")
        System.setProperty("INTEGRASJON_TILGANG_SCOPE", "scope")

        // Opppgave
        System.setProperty("INTEGRASJON_OPPGAVE_URL", "http://localhost:${oppgave.port()}")
        System.setProperty("INTEGRASJON_OPPGAVE_SCOPE", "scope")

        // Meldekortbackend
        System.setProperty("AZP_MELDEKORT_BACKEND", UUID.randomUUID().toString())
        System.setProperty("AZP_SAAS_PROXY", UUID.randomUUID().toString())
        System.setProperty("AZP_TOKEN_GEN", UUID.randomUUID().toString())
        System.setProperty("AZP_TILLEGGSSTONADER_INTEGRASJONER", UUID.randomUUID().toString())
    }
}

@Suppress("PropertyName")
data class TestToken(
    val access_token: String,
    val refresh_token: String = "very.secure.token",
    val id_token: String = "very.secure.token",
    val token_type: String = "token-type",
    val scope: String? = null,
    val expires_in: Int = 3599,
)

fun Application.texas() {
    install(ContentNegotiation) { jackson() }
    routing {
        post("/token") {
            call.respond(TestToken(AzureTokenGen("test", "test").generate(isApp = true)))
        }
        post("/token/exchange") {
            call.respond(TestToken(AzureTokenGen("test", "test").generate(isApp = false)))
        }
        post("/introspect") {
            call.respond(mapOf("active" to true))
        }
    }
}

fun Application.arena() {
    install(ContentNegotiation) { jackson() }
    routing {
        post("/intern/maksimum") {
            call.respond(Maksimum(vedtak = emptyList()))
        }
        post("/intern/person/saker") {
            call.respond(SakerResponse(emptyList()))
        }
    }
}

fun Application.oppgaveFake() {
    install(ContentNegotiation) { jackson() }
    routing {
        post("/enhet/status/person") {
            call.receive<PersonRequest>()
            call.respond(
                EnhetOgOversendelse(
                    tilstand = null
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
