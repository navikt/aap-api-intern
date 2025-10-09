package api.meldekort

import api.TestConfig
import api.api
import api.maksimum.dataSource
import api.util.MockedArenaClient
import api.util.AzureTokenGen
import api.util.Fakes
import api.util.PdlClientEmpty
import api.util.PostgresTestBase
import api.util.localDate
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
import no.nav.aap.api.intern.MeldekortDetaljerRequest
import no.nav.aap.api.intern.MeldekortDetaljerResponse
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.ArbeidIPeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DetaljertMeldekortDTO
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

val testObject = DetaljertMeldekortDTO(
    personIdent = "12345678901",
    saksnummer = Saksnummer("asd123"),
    mottattTidspunkt = LocalDateTime.now(),
    journalpostId = "1234",
    meldeperiodeFom = localDate("2023-01-01"),
    meldeperiodeTom = localDate("2023-01-15"),
    behandlingId = 1234,
    timerArbeidPerPeriode = listOf(ArbeidIPeriodeDTO(localDate("2023-01-01"), localDate("2023-01-01"), 2.5.toBigDecimal())),
    meldepliktStatusKode = null,
    rettighetsTypeKode = null,
    avslagsårsakKode = null,
)

class MeldekortDetaljerRepositoryTest : PostgresTestBase(dataSource) {
    companion object {
        private val fakes = Fakes()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            Fakes().close()
        }
    }

    @Test
    fun `kan lagre og hente meldekort detaljer`() {
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

            val res = jsonHttpClient.post("/api/insert/meldekort-detaljer") {
                bearerAuth(azure.generate(isApp = true))
                contentType(ContentType.Application.Json)
                setBody(
                    listOf(testObject)
                )
            }

            assertEquals(HttpStatusCode.OK, res.status)
            val meldekort = countMeldekort()
            assert(meldekort > 0)

            val meldekortResponse = jsonHttpClient.post("/kelvin/meldekort-detaljer"){
                bearerAuth(azure.generate(isApp = true))
                contentType(ContentType.Application.Json)
                setBody(MeldekortDetaljerRequest(
                    "12345678901",
                    fraOgMedDato = LocalDate.now().minusYears(3),
                    tilOgMedDato = LocalDate.now().plusDays(1)
                    ))
            }

            assert(meldekortResponse.status == HttpStatusCode.OK)
            val meldekortListe = meldekortResponse.body<MeldekortDetaljerResponse>()
            assert(meldekortListe.meldekort.isNotEmpty())
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