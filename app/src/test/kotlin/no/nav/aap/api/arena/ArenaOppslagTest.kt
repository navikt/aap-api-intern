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
import no.nav.aap.api.util.AzureTokenGen
import no.nav.aap.api.util.Fakes
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerRequest as SakerRequestV1
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test


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
        val config = TestConfig.default()
        val token = AzureTokenGen("test", "test")

        testApplication {
            application {
                api(
                    config = config,
                    datasource = dataSource,
                    arenaService = fakes.arenaService,
                    modiaProducer = fakes.kafka,
                    aapHendelseProducer = fakes.aapHendelse,
                )
            }
            testBlock(token)
        }
    }

    @Test
    fun `kan hente saker for person med saker`() {
        testWithKtorApp { token ->
            val res = jsonHttpClient.post("/arena/person/saker") {
                bearerAuth(token.generate(isApp = true))
                contentType(ContentType.Application.Json)
                setBody(SakerRequestV1(personidentifikator = "01410028596"))
            }
            assertThat(res).isNotNull()
            assertThat(res.status).isEqualTo(HttpStatusCode.OK)
            val parsedBody = res.body<SakerResponse>()
            assertThat(parsedBody).isNotNull
            assertThat(parsedBody.saker).isNotEmpty() // forventet respons fra FakeArenaGateway
        }
    }

    @Test
    fun `kan hente saker for person uten saker`() {
        testWithKtorApp { token ->
            val res = jsonHttpClient.post("/arena/person/saker") {
                bearerAuth(token.generate(isApp = true))
                contentType(ContentType.Application.Json)
                setBody(SakerRequestV1(personidentifikator = "12345678910"))
            }
            assertThat(res).isNotNull()
            assertThat(res.status).isEqualTo(HttpStatusCode.OK)
            val parsedBody = res.body<SakerResponse>()
            assertThat(parsedBody).isNotNull
            assertThat(parsedBody.saker).isEmpty() // forventet respons fra FakeArenaGateway
        }
    }

    @Test
    fun `kan hente saker for person som ikke finnes`() {
        testWithKtorApp { token ->
            val res = jsonHttpClient.post("/arena/person/saker") {
                bearerAuth(token.generate(isApp = true))
                contentType(ContentType.Application.Json)
                setBody(SakerRequestV1(personidentifikator = "finnes ikke"))
            }
            assertThat(res).isNotNull()
            assertThat(res.status).isEqualTo(HttpStatusCode.OK)
            val parsedBody = res.body<SakerResponse>()
            assertThat(parsedBody).isNotNull
            assertThat(parsedBody.saker).isEmpty() // forventet respons fra FakeArenaGateway
        }
    }

    @Test
    fun `hentSakerForPerson returnerer 400 når body mangler`() {
        testWithKtorApp { token ->
            val res = jsonHttpClient.post("/arena/person/saker") {
                bearerAuth(token.generate(isApp = true))
                contentType(ContentType.Application.Json)
            }
            assertThat(res.status.value).isGreaterThanOrEqualTo(400)
        }
    }

    @Test
    fun `hentSakerForPerson returnerer 401 uten token`() {
        testWithKtorApp { _ ->
            val res = jsonHttpClient.post("/arena/person/saker") {
                contentType(ContentType.Application.Json)
                setBody(SakerRequestV1(personidentifikator = "12345678910"))
            }
            assertThat(res.status).isEqualTo(HttpStatusCode.Unauthorized)
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
