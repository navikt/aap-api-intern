package no.nav.aap.api.meldekort

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
import no.nav.aap.api.intern.MeldekortDetaljerRequest
import no.nav.aap.api.intern.MeldekortDetaljerResponse
import no.nav.aap.api.maksimum.BehandlingsDataTest
import no.nav.aap.api.util.AzureTokenGen
import no.nav.aap.api.util.Fakes
import no.nav.aap.api.util.FakeArenaClient
import no.nav.aap.api.util.PdlClientEmpty
import no.nav.aap.api.util.PostgresTestBase
import no.nav.aap.api.util.localDate
import no.nav.aap.api.util.localDateTime
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.ArbeidIPeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DetaljertMeldekortDTO
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MeldekortDetaljerTest : PostgresTestBase() {
    companion object {
        val testMottatTidspunkt = localDateTime("2024-01-01 12:34:00")!!
        val testFom = localDate("2023-01-01")
        val testTom = localDate("2023-01-15")
        val testObject = DetaljertMeldekortDTO(
            personIdent = "12345678901",
            saksnummer = Saksnummer("asd123"),
            mottattTidspunkt = testMottatTidspunkt,
            journalpostId = "1234",
            meldeperiodeFom = testFom,
            meldeperiodeTom = testTom,
            behandlingId = 1234,
            timerArbeidPerPeriode = listOf(
                ArbeidIPeriodeDTO(
                    testFom,
                    testFom,
                    2.5.toBigDecimal()
                )
            ),
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
                    arenaService = fakes.arenaService,
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

            val meldekortResponse = jsonHttpClient.post("/kelvin/meldekort-detaljer") {
                bearerAuth(azure.generate(isApp = true))
                contentType(ContentType.Application.Json)
                setBody(
                    MeldekortDetaljerRequest(
                        "12345678901",
                        fraOgMedDato = testFom.minusWeeks(1),
                        tilOgMedDato = LocalDate.now().plusDays(1)
                    )
                )
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