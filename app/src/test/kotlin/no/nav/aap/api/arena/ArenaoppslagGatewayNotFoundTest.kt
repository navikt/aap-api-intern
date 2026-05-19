package no.nav.aap.api.arena

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import no.nav.aap.api.ArenaoppslagConfig
import no.nav.aap.api.util.TestToken
import no.nav.aap.api.util.AzureTokenGen
import no.nav.aap.api.util.port
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerRequest as SakerRequestV1

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArenaoppslagGatewayNotFoundTest {

    private var arenaResponse: (suspend (call: io.ktor.server.application.ApplicationCall) -> Unit) = { call ->
        call.respond(SakerResponse(emptyList()))
    }

    private val texas = embeddedServer(Netty, port = 0) {
        install(ContentNegotiation) { jackson() }
        routing {
            post("/token") {
                call.respond(TestToken(AzureTokenGen("test", "test").generate(isApp = true)))
            }
        }
    }.start()

    private val arena = embeddedServer(Netty, port = 0) {
        install(ContentNegotiation) { jackson() }
        routing {
            post("/api/v1/person/saker") { arenaResponse(call) }
        }
    }.start()

    @BeforeAll
    fun setup() {
        System.setProperty("nais.token.endpoint", "http://localhost:${texas.port()}/token")
    }

    @AfterAll
    fun tearDown() {
        texas.stop(0L, 0L)
        arena.stop(0L, 0L)
    }

    private fun gateway() = ArenaoppslagGateway(
        arenaoppslagConfig = ArenaoppslagConfig(
            proxyBaseUrl = "http://localhost:${arena.port()}",
            scope = "test",
        ),
    )

    @Test
    fun `hentSakerForPerson returnerer tom liste når Arena svarer 404 Not Found`() = runBlocking {
        arenaResponse = { call ->
            call.respondText(
                text = "\"Fant ikke personen i Arena\"",
                contentType = io.ktor.http.ContentType.Application.Json,
                status = HttpStatusCode.NotFound,
            )
        }

        val response = gateway().hentSakerForPerson(
            callId = "test-call-id",
            req = SakerRequestV1(personidentifikator = "12345678910"),
        )

        assertThat(response).isNotNull
        assertThat(response.saker).isEmpty()
    }

    @Test
    fun `hentSakerForPerson kaster videre når Arena svarer 500`() {
        arenaResponse = { call ->
            call.respondText(
                text = "boom",
                status = HttpStatusCode.InternalServerError,
            )
        }

        assertThrows<Throwable> {
            runBlocking {
                gateway().hentSakerForPerson(
                    callId = "test-call-id",
                    req = SakerRequestV1(personidentifikator = "12345678910"),
                )
            }
        }
    }

    @Test
    fun `hentSakerForPerson returnerer saker når Arena svarer 200`() = runBlocking {
        arenaResponse = { call ->
            call.respond(SakerResponse(emptyList()))
        }

        val response = gateway().hentSakerForPerson(
            callId = "test-call-id",
            req = SakerRequestV1(personidentifikator = "12345678910"),
        )

        assertThat(response.saker).isEmpty()
    }
}

