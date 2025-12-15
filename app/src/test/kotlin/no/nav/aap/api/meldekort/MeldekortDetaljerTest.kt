package no.nav.aap.api.meldekort

import no.nav.aap.api.TestConfig
import no.nav.aap.api.api
import no.nav.aap.api.util.MockedArenaClient
import no.nav.aap.api.util.AzureTokenGen
import no.nav.aap.api.util.Fakes
import no.nav.aap.api.util.PdlClientEmpty
import no.nav.aap.api.util.PostgresTestBase
import no.nav.aap.api.util.localDate
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime


class MeldekortDetaljerTest : PostgresTestBase() {
    companion object {
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

        private val fakes = Fakes()

        @AfterAll
        @JvmStatic
        fun afterAll() {
            Fakes().close()
        }
    }

    @Test
    fun `kan lagre og hente meldekort-detaljer`() {
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
            assertThat(meldekort > 0).isTrue

            val meldekortResponse = jsonHttpClient.post("/kelvin/meldekort-detaljer"){
                bearerAuth(azure.generate(isApp = true))
                contentType(ContentType.Application.Json)
                setBody(MeldekortDetaljerRequest(
                    "12345678901",
                    fraOgMedDato = LocalDate.now().minusYears(3),
                    tilOgMedDato = LocalDate.now().plusDays(1)
                    ))
            }

            assertThat(meldekortResponse.status).isEqualTo(HttpStatusCode.OK)
            val meldekortListe = meldekortResponse.body<MeldekortDetaljerResponse>()
            assertThat(meldekortListe.meldekort).isNotEmpty()
            assertThat(meldekortListe.meldekort.first().meldePeriode.fraOgMedDato).isEqualTo(testObject.meldeperiodeFom)
            assertThat(meldekortListe.meldekort.first().meldePeriode.tilOgMedDato).isEqualTo(testObject.meldeperiodeTom)
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