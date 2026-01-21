package no.nav.aap.api.arena

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import no.nav.aap.api.TestConfig
import no.nav.aap.api.api
import no.nav.aap.api.intern.PersonEksistererIAAPArena
import no.nav.aap.api.intern.PersonHarSignifikantAAPArenaHistorikk
import no.nav.aap.api.util.AzureTokenGen
import no.nav.aap.api.util.Fakes
import no.nav.aap.api.util.MockedArenaClient
import no.nav.aap.arenaoppslag.kontrakt.intern.KanBehandleSoknadIKelvin
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.time.LocalDate


class ArenaOppslagTest {
    companion object {

        private val fakes = Fakes()
        private val dataSource = TestDataSource()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            Fakes().close()
            dataSource.close()
        }
    }

    fun testWithKtorApp(testBlock: suspend ApplicationTestBuilder.(azure: AzureTokenGen) -> Unit) {
        val config = TestConfig.default(fakes)
        val token = AzureTokenGen("test", "test")

        testApplication {
            application {
                api(
                    config = config,
                    datasource = dataSource,
                    arenaRestClient = MockedArenaClient(),
                    modiaProducer = fakes.kafka
                )
            }
            testBlock(token)
        }
    }

    @Test
    fun `kan kalle på aap-eksisterer`() {
        testWithKtorApp { token ->
            val res = jsonHttpClient.post("/arena/person/aap/eksisterer") {
                bearerAuth(token.generate(isApp = true))
                contentType(ContentType.Application.Json)
                setBody(SakerRequest(personidentifikatorer = listOf("12345678910")))
            }
            assertThat(res).isNotNull()
            assertThat(res.status).isEqualTo(HttpStatusCode.OK)
            val parsedBody = res.body<PersonEksistererIAAPArena>()
            assertThat(parsedBody).isNotNull
            assertThat(parsedBody.eksisterer).isFalse // forventet respons fra MockedArenaClient
        }
    }

    @Test
    fun `kan kalle på kan behandles i Kelvin`() {
        testWithKtorApp { token ->
            val res = jsonHttpClient.post("/arena/person/aap/signifikant-historikk") {
                bearerAuth(token.generate(isApp = true))
                contentType(ContentType.Application.Json)
                setBody(KanBehandleSoknadIKelvin(listOf("12345678910"), LocalDate.now()))
            }
            assertThat(res).isNotNull()
            assertThat(res.status).isEqualTo(HttpStatusCode.OK)
            val parsedBody = res.body<PersonHarSignifikantAAPArenaHistorikk>()
            assertThat(parsedBody).isNotNull
            assertThat(parsedBody.harSignifikantHistorikk).isFalse // forventet respons fra MockedArenaClient
        }
    }

    private val ApplicationTestBuilder.jsonHttpClient: HttpClient
        get() =
            createClient {
                install(ContentNegotiation) {
                    jackson {
                        registerModule(JavaTimeModule())
                        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    }
                }
            }
}