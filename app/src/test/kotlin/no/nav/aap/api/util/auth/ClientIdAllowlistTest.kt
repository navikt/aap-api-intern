package no.nav.aap.api.util.auth

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.DuplicatePluginException
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.aap.api.util.TokenXTokenGen
import no.nav.aap.api.util.WithFakes
import no.nav.aap.komponenter.server.auth.IdentityProvider
import no.nav.aap.komponenter.server.authentication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

@WithFakes
class ClientIdAllowlistTest {
    private val godkjent = "localhost:aap:innsyn"
    private val annenGodkjent = "localhost:aap:annen-frontend"
    private val tokenx = TokenXTokenGen()
    private val handlerKall = AtomicInteger(0)

    private fun ApplicationTestBuilder.oppsett(vararg tillatte: String = arrayOf(godkjent)) {
        application {
            install(ContentNegotiation) { jackson() }
            authentication(listOf(IdentityProvider.TOKENX))
            routing {
                authenticate(IdentityProvider.TOKENX.value) {
                    route("/beskyttet") {
                        installClientIdAllowlist(ClientIdAllowlistConfig(tillatte.toList()))
                        get {
                            handlerKall.incrementAndGet()
                            call.respondText("ok")
                        }
                        get("/nostet") {
                            handlerKall.incrementAndGet()
                            call.respondText("ok")
                        }
                    }
                    get("/ubeskyttet") {
                        handlerKall.incrementAndGet()
                        call.respondText("ok")
                    }
                }
            }
        }
    }

    @Test
    fun `slipper gjennom godkjent client_id`() = testApplication {
        oppsett()

        val response = client.get("/beskyttet") { bearerAuth(tokenx.generate(clientId = godkjent)) }

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
        assertThat(response.bodyAsText()).isEqualTo("ok")
        assertThat(handlerKall.get()).isEqualTo(1)
    }

    @Test
    fun `avviser client_id som ikke er i allowlist uten å kjøre handleren`() = testApplication {
        oppsett()

        val response = client.get("/beskyttet") { bearerAuth(tokenx.generate(clientId = "localhost:annet-team:ukjent-app")) }

        assertThat(response.status).isEqualTo(HttpStatusCode.Forbidden)
        assertThat(handlerKall.get()).isZero()
    }

    @Test
    fun `avviser token uten client_id-claim uten å kjøre handleren`() = testApplication {
        oppsett()

        val response = client.get("/beskyttet") { bearerAuth(tokenx.generate(clientId = null)) }

        assertThat(response.status).isEqualTo(HttpStatusCode.Forbidden)
        assertThat(handlerKall.get()).isZero()
    }

    @Test
    fun `slipper gjennom alle client_id-er når allowlisten har flere`() = testApplication {
        oppsett(godkjent, annenGodkjent)

        val første = client.get("/beskyttet") { bearerAuth(tokenx.generate(clientId = godkjent)) }
        val andre = client.get("/beskyttet") { bearerAuth(tokenx.generate(clientId = annenGodkjent)) }

        assertThat(første.status).isEqualTo(HttpStatusCode.OK)
        assertThat(andre.status).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    fun `beskytter nøstede ruter under ruten pluginen er installert på`() = testApplication {
        oppsett()

        val godkjentResponse = client.get("/beskyttet/nostet") { bearerAuth(tokenx.generate(clientId = godkjent)) }
        val avvistResponse = client.get("/beskyttet/nostet") { bearerAuth(tokenx.generate(clientId = "localhost:aap:ukjent")) }

        assertThat(godkjentResponse.status).isEqualTo(HttpStatusCode.OK)
        assertThat(avvistResponse.status).isEqualTo(HttpStatusCode.Forbidden)
    }

    @Test
    fun `påvirker ikke ruter utenfor ruten pluginen er installert på`() = testApplication {
        oppsett()

        val response = client.get("/ubeskyttet") { bearerAuth(tokenx.generate(clientId = "localhost:aap:ukjent")) }

        assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    fun `gir 401 uten token, før pluginen kjører`() = testApplication {
        oppsett()

        val response = client.get("/beskyttet")

        assertThat(response.status).isEqualTo(HttpStatusCode.Unauthorized)
        assertThat(handlerKall.get()).isZero()
    }

    @Test
    fun `returnerer feilmelding i samme format som resten av APIet`() = testApplication {
        oppsett()

        val response = client.get("/beskyttet") { bearerAuth(tokenx.generate(clientId = "localhost:aap:ukjent")) }

        assertThat(response.bodyAsText()).contains("Ingen tilgang, client_id ikke godkjent")
    }

    @Test
    fun `krever eksakt match på client_id`() = testApplication {
        oppsett()

        val medSuffiks = client.get("/beskyttet") { bearerAuth(tokenx.generate(clientId = "$godkjent-x")) }
        val annenCase = client.get("/beskyttet") { bearerAuth(tokenx.generate(clientId = godkjent.uppercase())) }
        val prefiks = client.get("/beskyttet") { bearerAuth(tokenx.generate(clientId = "localhost:aap")) }

        assertThat(medSuffiks.status).isEqualTo(HttpStatusCode.Forbidden)
        assertThat(annenCase.status).isEqualTo(HttpStatusCode.Forbidden)
        assertThat(prefiks.status).isEqualTo(HttpStatusCode.Forbidden)
        assertThat(handlerKall.get()).isZero()
    }

    @Test
    fun `kaster ved dobbel installasjon på samme rute`() = testApplication {
        var feil: Throwable? = null
        application {
            routing {
                route("/dobbel") {
                    val config = ClientIdAllowlistConfig(listOf(godkjent))
                    installClientIdAllowlist(config)
                    feil = runCatching { installClientIdAllowlist(config) }.exceptionOrNull()
                }
            }
        }

        startApplication()

        assertThat(feil).isInstanceOf(DuplicatePluginException::class.java)
    }
}
