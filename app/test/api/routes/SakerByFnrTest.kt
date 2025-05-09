package api.routes

import api.TestConfig
import api.api
import api.kelvin.SakStatus
import api.kelvin.SakStatusKelvin
import api.util.ArenaClient
import api.util.AzureTokenGen
import api.util.Fakes
import api.util.PdlClientEmpty
import api.util.PostgresTestBase
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.aap.api.intern.Kilde
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.Status
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.use

val dataSource = InitTestDatabase.freshDatabase()

class SakerByFnrTest : PostgresTestBase(dataSource) {
    private val kelvinSak =
        SakStatusKelvin(
            ident = "12345678910",
            status =
                SakStatus(
                    sakId = "1234",
                    statusKode = Status.IVERK,
                    periode =
                        Periode(
                            fom = LocalDate.ofYearDay(2021, 1),
                            tom =
                                LocalDate.ofYearDay(
                                    2021,
                                    31,
                                ),
                        ),
                    kilde = Kilde.KELVIN,
                ),
        )

    @Test
    fun `kan hente sakerByFnr med obo-token og m2m-token`() {
        Fakes().use { fakes ->

            val config = TestConfig.default(fakes)
            val azure = AzureTokenGen("test", "test")

            testApplication {
                application {
                    api(
                        config = config,
                        datasource = dataSource,
                        arenaRestClient = ArenaClient(),
                        pdlClient = PdlClientEmpty(),
                    )
                }

                val res =
                    jsonHttpClient.post("/api/insert/sakStatus") {
                        bearerAuth(azure.generate(isApp = true))
                        contentType(ContentType.Application.Json)
                        setBody(kelvinSak)
                    }
                assertEquals(HttpStatusCode.OK, res.status)

                val oboResponse =
                    jsonHttpClient.post("/sakerByFnr") {
                        bearerAuth(OidcToken(azure.generate(isApp = false)).token())
                        contentType(ContentType.Application.Json)
                        setBody(SakerRequest(personidentifikatorer = listOf("12345678910")))
                    }
                assertEquals(HttpStatusCode.OK, oboResponse.status)

                val m2mResponse =
                    jsonHttpClient.post("/sakerByFnr") {
                        bearerAuth(azure.generate(isApp = true))
                        contentType(ContentType.Application.Json)
                        setBody(SakerRequest(personidentifikatorer = listOf("12345678910")))
                    }
                assertEquals(HttpStatusCode.OK, m2mResponse.status)
            }
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
