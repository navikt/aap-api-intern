package api.meldekortperioder

import api.TestConfig
import api.api
import api.kelvin.MeldekortPerioderDTO
import api.util.ArenaClient
import api.util.AzureTokenGen
import api.util.Fakes
import api.util.PostgresTestBase.clearTables
import api.util.PostgresTestBase.countMeldekortEntries
import api.util.PostgresTestBase.dataSource
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class MeldekortPeriodeTest {
    companion object {
        lateinit var httpClient: HttpClient
        val azure = AzureTokenGen("test", "test")

        @BeforeEach
        fun beforeEach() {
            clearTables()
        }

        @JvmStatic
        @BeforeAll
        fun setup() {
            val testApplication = TestApplication {
                application {
                    Fakes().use { fakes ->
                        api(
                            config = TestConfig.default(fakes),
                            datasource = dataSource,
                            arenaRestClient = ArenaClient()
                        )
                    }
                }
            }

            httpClient = testApplication.createClient {
                install(ContentNegotiation) {
                    jackson {
                        registerModule(JavaTimeModule())
                        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    }
                }
            }
        }
    }

    @Test
    fun `kan lagre ned og hente meldekort perioder`() {
        val perioder = listOf(
            Periode(LocalDate.ofYearDay(2021, 1), LocalDate.ofYearDay(2021, 15)),
            Periode(LocalDate.ofYearDay(2021, 16), LocalDate.ofYearDay(2021, 31))
        )

        runBlocking {
            val response = httpClient.post("/api/insert/meldeperioder") {
                bearerAuth(azure.generate(true))
                contentType(ContentType.Application.Json)
                setBody(
                    MeldekortPerioderDTO(
                        "12345678910", perioder
                    )
                )
            }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(2, countMeldekortEntries())

            val meldekortPerioderRes = httpClient.post("/perioder/meldekort") {
                bearerAuth(azure.generate(true))
                contentType(ContentType.Application.Json)
                setBody(
                    InternVedtakRequest(
                        "12345678910", LocalDate.ofYearDay(2021, 1), LocalDate.ofYearDay(2021, 31)
                    )
                )
            }

            assert(meldekortPerioderRes.status.isSuccess())
            Assertions.assertEquals(meldekortPerioderRes.body<List<Periode>>(), perioder)
        }
    }
}
