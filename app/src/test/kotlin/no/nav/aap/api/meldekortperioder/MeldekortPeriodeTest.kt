package no.nav.aap.api.meldekortperioder

import no.nav.aap.api.TestConfig
import no.nav.aap.api.api
import no.nav.aap.api.kelvin.MeldekortPerioderDTO
import no.nav.aap.api.util.MockedArenaClient
import no.nav.aap.api.util.AzureTokenGen
import no.nav.aap.api.util.Fakes
import no.nav.aap.api.util.PdlClientEmpty
import no.nav.aap.api.util.PostgresTestBase
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
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MeldekortPeriodeTest : PostgresTestBase() {

    @Test
    fun `kan lagre ned og hente meldekortperioder`() {
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
                        datasource = dataSource,
                        arenaRestClient = MockedArenaClient(),
                        pdlClient = PdlClientEmpty(),
                        modiaProducer = fakes.kafka
                    )
                }
                //Disabled OBO TEST, SJEKK VED LEDIG KAPASITET
                val aktivitetsfaseResObo = jsonHttpClient.post("/perioder/aktivitetfase") {
                    bearerAuth(OidcToken(azure.generate(isApp = true)).token())
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