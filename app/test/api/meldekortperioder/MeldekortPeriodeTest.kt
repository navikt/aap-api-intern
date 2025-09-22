package api.meldekortperioder

import api.TestConfig
import api.api
import api.kelvin.MeldekortPerioderDTO
import api.util.ArenaClient
import api.util.AzureTokenGen
import api.util.Fakes
import api.util.PdlClientEmpty
import api.util.PostgresTestBase
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

private val dataSource2 = InitTestDatabase.freshDatabase()

class MeldekortPeriodeTest : PostgresTestBase(dataSource2) {

    @Test
    fun `kan lagre ned og hente meldekortperioder`() {
        Fakes().use { fakes ->
            val config = TestConfig.default(fakes)
            val azure = AzureTokenGen("test", "test")

            testApplication {
                application {
                    api(
                        config = config,
                        datasource = dataSource2,
                        arenaRestClient = ArenaClient(),
                        pdlClient = PdlClientEmpty(),
                        modiaProducer = fakes.kafka
                    )
                }

                val perioder = listOf(
                    Periode(LocalDate.ofYearDay(2021, 1), LocalDate.ofYearDay(2021, 15)),
                    Periode(LocalDate.ofYearDay(2021, 16), LocalDate.ofYearDay(2021, 31))
                )

                val res = jsonHttpClient.post("/api/insert/meldeperioder") {
                    bearerAuth(azure.generate(isApp = true))
                    contentType(ContentType.Application.Json)
                    setBody(
                        MeldekortPerioderDTO(
                            "12345678910",
                            perioder
                        )
                    )
                }

                assertEquals(HttpStatusCode.OK, res.status)
                assertEquals(countMeldekortEntries(), 2)

                val meldekortPerioderResM2m = jsonHttpClient.post("/perioder/meldekort") {
                    bearerAuth(azure.generate(isApp = true))
                    contentType(ContentType.Application.Json)
                    setBody(InternVedtakRequest("12345678910", LocalDate.ofYearDay(2021, 1), LocalDate.ofYearDay(2021, 31)))
                }
                assert(meldekortPerioderResM2m.status.isSuccess())
                assertEquals(meldekortPerioderResM2m.body<List<Periode>>(), perioder)

                val meldekortPerioderResObo = jsonHttpClient.post("/perioder/meldekort") {
                    bearerAuth(OidcToken(azure.generate(isApp = false)).token())
                    contentType(ContentType.Application.Json)
                    setBody(InternVedtakRequest("12345678910", LocalDate.ofYearDay(2021, 1), LocalDate.ofYearDay(2021, 31)))
                }
                assert(meldekortPerioderResObo.status.isSuccess())
            }
        }
    }

    @Test
    fun `kan lagre ned og hente aktivitetfase`() {
        Fakes().use { fakes ->

            val config = TestConfig.default(fakes)
            val azure = AzureTokenGen("test", "test")

            testApplication {
                application {
                    api(
                        config = config,
                        datasource = dataSource2,
                        arenaRestClient = ArenaClient(),
                        pdlClient = PdlClientEmpty(),
                        modiaProducer = fakes.kafka
                    )
                }

                val aktivitetsfaseResObo = jsonHttpClient.post("/perioder/aktivitetfase") {
                    bearerAuth(OidcToken(azure.generate(isApp = false)).token())
                    contentType(ContentType.Application.Json)
                    setBody(InternVedtakRequest("12345678910", LocalDate.ofYearDay(2021, 1), LocalDate.ofYearDay(2021, 31)))
                }
                assert(aktivitetsfaseResObo.status.isSuccess())
                assertEquals(aktivitetsfaseResObo.body<PerioderMed11_17Response>().perioder, listOf<Periode>())

                val aktivitetsfaseResM2m = jsonHttpClient.post("/perioder/aktivitetfase") {
                    bearerAuth(azure.generate(isApp = true))
                    contentType(ContentType.Application.Json)
                    setBody(InternVedtakRequest("12345678910", LocalDate.ofYearDay(2021, 1), LocalDate.ofYearDay(2021, 31)))
                }
                assert(aktivitetsfaseResM2m.status.isSuccess())
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