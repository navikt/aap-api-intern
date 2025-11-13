package no.nav.aap.api.meldekortperioder

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
import no.nav.aap.api.intern.Kilde
import no.nav.aap.api.intern.SakStatus
import no.nav.aap.api.kelvin.SakStatusKelvin
import no.nav.aap.api.util.AzureTokenGen
import no.nav.aap.api.util.Fakes
import no.nav.aap.api.util.MockedArenaClient
import no.nav.aap.api.util.PdlClientEmpty
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.Status
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.time.LocalDate

@Execution(ExecutionMode.SAME_THREAD)
class SakStatusKelvinTest {

    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() = dataSource.close()


    companion object {
        val kelvinSak = SakStatusKelvin(
            ident = "12345678910",
            status = no.nav.aap.api.kelvin.SakStatus(
                sakId = "1234",
                statusKode = Status.IVERK,
                periode = Periode(
                    fom = LocalDate.ofYearDay(2021, 1),
                    tom = LocalDate.ofYearDay(
                        2021, 31
                    )
                ),
                kilde = Kilde.KELVIN,
            )
        )
    }

    @Test
    fun `kan lagre ned og hente saker`() {
        Fakes().use { fakes ->
            val config = TestConfig.default(fakes)
            val azure = AzureTokenGen("test", "test")

            testApplication {
                application {
                    api(
                        config = config,
                        datasource = dataSource,
                        arenaRestClient = MockedArenaClient(),
                        pdlClient = PdlClientEmpty(),
                        modiaProducer = fakes.kafka
                    )
                }

                val res = jsonHttpClient.post("/api/insert/sakStatus") {
                    bearerAuth(azure.generate(true))
                    contentType(ContentType.Application.Json)
                    setBody(kelvinSak)
                }

                assertEquals(HttpStatusCode.OK, res.status)
                assertThat(countSaker()).isEqualTo(1)


                val oboResponse =
                    jsonHttpClient.post("/sakerByFnr") {
                        bearerAuth(OidcToken(azure.generate(isApp = true)).token())
                        contentType(ContentType.Application.Json)
                        setBody(SakerRequest(personidentifikatorer = listOf("12345678910")))
                    }
                assertEquals(HttpStatusCode.OK, oboResponse.status)
                assertEquals(
                    oboResponse.body<List<SakStatus>>().first().sakId,
                    kelvinSak.status.sakId
                )

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

    fun countSaker(): Int? =
        dataSource.transaction { con ->
            con.queryFirstOrNull("SELECT count(*) as nr FROM SAKER") {
                setRowMapper { row -> row.getInt("nr") }
            }
        }
}