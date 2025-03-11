package api.meldekortperioder

import api.TestConfig
import api.api
import api.arena.ArenaoppslagRestClient
import api.kelvin.SakStatusKelvin
import api.util.ArenaClient
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

class SakStatusKelvinTest : PostgresTestBase() {
    @Test
    fun `kan lagre ned sak status`() {
        Fakes().use { fakes ->
            val config = TestConfig.default(fakes)
            val azure = AzureTokenGen("test", "test")

            testApplication {
                application {
                    api(
                        config = config,
                        datasource = InitTestDatabase.dataSource,
                        arenaRestClient = ArenaClient()
                    )
                }


                val res = jsonHttpClient.post("/api/insert/sakStatus") {
                    bearerAuth(azure.generate(true))
                    contentType(ContentType.Application.Json)
                    setBody(
                        SakStatusKelvin(
                            ident ="12345678910",
                            status = api.kelvin.SakStatus(
                                sakId = "1234",
                                statusKode = no.nav.aap.arenaoppslag.kontrakt.intern.Status.IVERK,
                                periode = Periode(
                                    fom = LocalDate.ofYearDay(2021, 1),
                                    tom = LocalDate.ofYearDay(
                                        2021, 31
                                    )
                                ),
                                kilde = no.nav.aap.api.intern.Kilde.KELVIN
                            )
                        )
                    )
                }

                assertEquals(HttpStatusCode.OK, res.status)
                assertEquals(countSaker(), 1)
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