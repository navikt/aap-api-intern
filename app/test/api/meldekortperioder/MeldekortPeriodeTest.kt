package api.meldekortperioder

import api.TestConfig
import api.api
import api.kelvin.MeldekortPerioderDTO
import api.util.AzureTokenGen
import api.util.Fakes
import api.util.PostgresTestBase
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate


class MeldekortPeriodeTest : PostgresTestBase() {

    @Test
    fun `kan lagre ned og hente meldekort perioder`() {
        Fakes().use { fakes ->
            val config = TestConfig.default(fakes)
            val azure = AzureTokenGen("test", "test")

            testApplication {
                application {
                    api(
                        config = config,
                        datasource = InitTestDatabase.dataSource
                    )
                }

                val res = jsonHttpClient.post("/api/insert/meldeperioder") {
                    bearerAuth(azure.generate(true))
                    contentType(ContentType.Application.Json)
                    setBody(
                        MeldekortPerioderDTO(
                            "12345678910",
                            listOf(
                                Periode(LocalDate.ofYearDay(2021, 1), LocalDate.ofYearDay(2021, 15)),
                                Periode(LocalDate.ofYearDay(2021, 16), LocalDate.ofYearDay(2021, 31))
                            )
                        )
                    )
                }

                assertEquals(HttpStatusCode.OK, res.status)
                assertEquals(countMeldekortEntries(), 2)
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