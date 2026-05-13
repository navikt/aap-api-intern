package no.nav.aap.api.kafka

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
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
import no.nav.aap.api.TestConfig
import no.nav.aap.api.api
import no.nav.aap.api.util.AzureTokenGen
import no.nav.aap.api.util.Fakes
import no.nav.aap.api.util.PostgresTestBase
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DatadelingDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.RettighetsTypePeriode
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.SakDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.TilkjentDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals

class AapHendelseIntegrasjonTest : PostgresTestBase() {

    companion object {
        val vedtakDTO = DatadelingDTO(
            behandlingsId = "123456789",
            behandlingsReferanse = "1234567890987654321",
            rettighetsPeriodeFom = LocalDate.now().minusYears(2),
            rettighetsPeriodeTom = LocalDate.now().minusYears(1),
            behandlingStatus = Status.IVERKSETTES,
            vedtaksDato = LocalDate.now().minusYears(2).plusDays(20),
            sak = SakDTO(
                saksnummer = "test",
                fnr = listOf("12345678910"),
                opprettetTidspunkt = LocalDateTime.now().minusYears(2),
            ),
            tilkjent = listOf(
                TilkjentDTO(
                    tilkjentFom = LocalDate.now().minusYears(2),
                    tilkjentTom = LocalDate.now().minusYears(1),
                    dagsats = 200,
                    gradering = 100,
                    grunnlagsfaktor = 2.4.toBigDecimal(),
                    grunnbeløp = 123321.toBigDecimal(),
                    antallBarn = 0,
                    barnetilleggsats = 0.toBigDecimal(),
                    barnetillegg = 0.toBigDecimal(),
                )
            ),
            rettighetsTypeTidsLinje = listOf(
                RettighetsTypePeriode(
                    fom = LocalDate.now().minusYears(2),
                    tom = LocalDate.now().minusYears(1),
                    verdi = RettighetsType.BISTANDSBEHOV.name,
                )
            ),
            samId = "test",
            vedtakId = 123456789L,
            beregningsgrunnlag = BigDecimal.valueOf(500_000),
            stansOpphørVurdering = null,
            arenavedtak = emptyList(),
        )
    }

    @Test
    fun `vedtak-endepunkt produserer VEDTAK-hendelse på kafka`() {
        Fakes().use { fakes ->
            val azure = AzureTokenGen("test", "test")

            testApplication {
                application {
                    api(
                        config = TestConfig.default(),
                        datasource = dataSource,
                        arenaService = fakes.arenaService,
                        modiaProducer = fakes.kafka,
                        aapHendelseProducer = fakes.aapHendelse,
                    )
                }

                val res = jsonHttpClient.post("/api/insert/vedtak") {
                    bearerAuth(azure.generate(isApp = true))
                    contentType(ContentType.Application.Json)
                    setBody(vedtakDTO)
                }

                assertEquals(HttpStatusCode.OK, res.status)

                val melding = pollForMessage(fakes, fnr = "12345678910")
                assertThat(melding).isNotNull
                assertThat(melding!!.second).isEqualTo(Hendelse.VEDTAK)
                assertThat(melding.first).isEqualTo("12345678910")
            }
        }
    }

    @Test
    fun `vedtak-endepunkt produserer hendelse med riktig fnr som kafka-nøkkel`() {
        Fakes().use { fakes ->
            val azure = AzureTokenGen("test", "test")
            val forventetFnr = "12345678910"

            testApplication {
                application {
                    api(
                        config = TestConfig.default(),
                        datasource = dataSource,
                        arenaService = fakes.arenaService,
                        modiaProducer = fakes.kafka,
                        aapHendelseProducer = fakes.aapHendelse,
                    )
                }

                jsonHttpClient.post("/api/insert/vedtak") {
                    bearerAuth(azure.generate(isApp = true))
                    contentType(ContentType.Application.Json)
                    setBody(vedtakDTO)
                }

                val melding = pollForMessage(fakes, fnr = forventetFnr)
                assertThat(melding?.first).isEqualTo(forventetFnr)
            }
        }
    }

    private fun pollForMessage(
        fakes: Fakes,
        fnr: String,
        timeoutMs: Long = 5000,
        pollIntervalMs: Long = 100,
    ): Pair<String, Hendelse>? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val msg = fakes.aapHendelse.sentMessages().firstOrNull { it.first == fnr }
            if (msg != null) return msg
            Thread.sleep(pollIntervalMs)
        }
        return null
    }

    private val ApplicationTestBuilder.jsonHttpClient: HttpClient
        get() = createClient {
            install(ContentNegotiation) {
                jackson {
                    registerModule(JavaTimeModule())
                    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                }
            }
        }
}
