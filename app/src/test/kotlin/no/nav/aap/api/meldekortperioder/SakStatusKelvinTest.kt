package no.nav.aap.api.meldekortperioder

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
import java.time.LocalDate
import no.nav.aap.api.TestConfig
import no.nav.aap.api.api
import no.nav.aap.api.intern.KelvinStatus
import no.nav.aap.api.intern.SakStatus
import no.nav.aap.api.intern.behandlingsflyt.Periode
import no.nav.aap.api.intern.behandlingsflyt.SakStatusKelvin
import no.nav.aap.api.intern.behandlingsflyt.SakstatusFraKelvin
import no.nav.aap.api.util.AzureTokenGen
import no.nav.aap.api.util.Fakes
import no.nav.aap.api.util.PdlGatewayEmpty
import no.nav.aap.api.util.WithFakes
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@WithFakes
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SakStatusKelvinTest {

    private lateinit var dataSource: TestDataSource

    @BeforeAll
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterAll
    fun tearDown() = dataSource.close()


    companion object {
        val kelvinSak = SakStatusKelvin(
            ident = "12345678910",
            status = no.nav.aap.api.intern.behandlingsflyt.SakStatus(
                sakId = "1234",
                statusKode = SakstatusFraKelvin.REVURDERING_UNDER_BEHANDLING,
                periode = Periode(
                    fom = LocalDate.ofYearDay(2021, 1),
                    tom = LocalDate.ofYearDay(
                        2021, 31
                    )
                ),
            )
        )
    }

    @Test
    fun `kan lagre ned og hente saker`() {
            val config = TestConfig.default()
            val azure = AzureTokenGen("test", "test")

            testApplication {
                application {
                    api(
                        config = config,
                        datasourceFactory = { dataSource },
                        arenaService = Fakes.getArenaService(),
                        modiaProducer = Fakes.getKafka(),
                        aapHendelseProducer = Fakes.getAapHendelse(),
                        pdlGateway = PdlGatewayEmpty(),
                    )
                }

                val res = jsonHttpClient.post("/api/insert/sakStatus") {
                    bearerAuth(azure.generate(true))
                    contentType(ContentType.Application.Json)
                    setBody(kelvinSak)
                }

                assertEquals(HttpStatusCode.OK, res.status)
                assertThat(countSaker()).isEqualTo(1)


                val response =
                    jsonHttpClient.post("/sakerByFnr") {
                        bearerAuth(azure.generate(isApp = false, azp = System.getProperty("AZP_SAAS_PROXY")))
                        contentType(ContentType.Application.Json)
                        setBody(SakerRequest(personidentifikatorer = listOf("12345678910")))
                    }
                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals(
                    SakStatus.Kelvin(
                        statusKode = KelvinStatus.REVURDERING_UNDER_BEHANDLING,
                        periode = no.nav.aap.api.intern.Periode(
                            fraOgMedDato = LocalDate.of(2021, 1, 1),
                            tilOgMedDato = LocalDate.of(2021, 1, 31)
                        ),
                        sakId = "1234",
                        perioder = emptyList(),
                        ytelsestatus = SakStatus.YtelseStatus.FOR_VEDTAK
                    ),
                    response.body<List<SakStatus>>().first(),
                )
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
