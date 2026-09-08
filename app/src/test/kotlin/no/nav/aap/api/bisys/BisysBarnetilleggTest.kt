package no.nav.aap.api.bisys

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
import io.ktor.server.application.Application
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import no.nav.aap.api.TestConfig
import no.nav.aap.api.api
import no.nav.aap.api.intern.BisysBarnMedBarnetillegg
import no.nav.aap.api.intern.BisysBarnetilleggRequest
import no.nav.aap.api.intern.BisysBarnetilleggResponse
import no.nav.aap.api.intern.BisysPeriodeMedBeløp
import no.nav.aap.api.kelvin.BarnMedBarnetillegg
import no.nav.aap.api.kelvin.Behandling
import no.nav.aap.api.kelvin.KelvinBehandlingStatus
import no.nav.aap.api.kelvin.PeriodeMedBeløp
import no.nav.aap.api.kelvin.Sak
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.api.util.AzureTokenGen
import no.nav.aap.api.util.Fakes
import no.nav.aap.api.util.PdlGatewayEmpty
import no.nav.aap.api.util.PostgresTestBase
import no.nav.aap.api.util.WithFakes
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@WithFakes
class BisysBarnetilleggTest : PostgresTestBase() {
    private val personidentifikator = "12345678910"
    private val barnIdent = "1234"
    private val sakId = "KELVIN-SAK"
    private val fom = LocalDate.of(2025, 1, 1)
    private val tom = LocalDate.of(2025, 12, 31)

    @Test
    fun `returnerer barn med barnetillegg for en person`() {
        lagreKelvinData()

        testApplication {
            application { defaultApi() }

            val response = post(BisysBarnetilleggRequest(personidentifikator))

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.body<BisysBarnetilleggResponse>().barnMedBarnetillegg).containsExactly(
                BisysBarnMedBarnetillegg(
                    ident = barnIdent,
                    perioderMedBarnetillegg = listOf(
                        BisysPeriodeMedBeløp(fra = fom, til = tom, beløp = BigDecimal(38)),
                    ),
                ),
                BisysBarnMedBarnetillegg(
                    ident = null,
                    perioderMedBarnetillegg = listOf(
                        BisysPeriodeMedBeløp(fra = fom, til = tom, beløp = BigDecimal(38)),
                    ),
                )
            )
        }
    }

    @Test
    fun `returnerer tom liste ved manglende data`() {
        testApplication {
            application { defaultApi() }

            val response = post(BisysBarnetilleggRequest("10987654321"))

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.body<BisysBarnetilleggResponse>().barnMedBarnetillegg).isEmpty()
        }
    }

    @Test
    fun `avviser uautorisert konsument`() {
        testApplication {
            application { defaultApi() }

            val response = jsonHttpClient.post("/bisys/barnetillegg") {
                bearerAuth(
                    AzureTokenGen("test", "test").generate(
                        isApp = false,
                        azp = UUID.randomUUID().toString(),
                    )
                )
                contentType(ContentType.Application.Json)
                setBody(BisysBarnetilleggRequest(personidentifikator))
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.Forbidden)
        }
    }

    private suspend fun ApplicationTestBuilder.post(requestBody: BisysBarnetilleggRequest) =
        jsonHttpClient.post("/bisys/barnetillegg") {
            bearerAuth(
                AzureTokenGen("test", "test").generate(
                    isApp = true,
                    azp = System.getProperty("AZP_BISYS"),
                )
            )
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

    private fun Application.defaultApi() {
        api(
            config = TestConfig.default(),
            datasourceFactory = { dataSource },
            arenaService = Fakes.getArenaService(),
            pdlGateway = PdlGatewayEmpty(),
            modiaProducer = Fakes.getKafka(),
            aapHendelseProducer = Fakes.getAapHendelse(),
        )
    }

    private fun lagreKelvinData() {
        dataSource.transaction { connection ->
            BehandlingsRepository(connection).lagreBehandling(
                listOf(personidentifikator),
                Behandling(
                    behandlingsReferanse = UUID.randomUUID().toString(),
                    rettighetsperiode = Periode(fom, tom),
                    behandlingStatus = KelvinBehandlingStatus.AVSLUTTET,
                    vedtaksDato = fom.minusDays(2),
                    sak = Sak(
                        sakId,
                        LocalDateTime.of(fom.minusMonths(2), java.time.LocalTime.NOON)
                    ),
                    tilkjent = tidslinjeOf(),
                    rettighetsTypePerioder = emptyList(),
                    samIdOgTpNr = emptyList(),
                    vedtakId = 123L,
                    beregningsgrunnlag = BigDecimal.ZERO,
                    nyttVedtak = true,
                    stansOpphørVurdering = emptySet(),
                    arenakompatibleVedtak = emptyList(),
                    foreløpigMaksdato = null,
                    perioderMedFritakMeldeplikt = emptyList(),
                    underveisperioder = emptyList(),
                    barnMedBarnetillegg = listOf(
                        BarnMedBarnetillegg(
                            ident = barnIdent,
                            perioderMedBarnetillegg = listOf(
                                PeriodeMedBeløp(fom = fom, tom = tom, beløp = BigDecimal(38)),
                            ),
                        ),
                        BarnMedBarnetillegg(
                            ident = null,
                            perioderMedBarnetillegg = listOf(
                                PeriodeMedBeløp(fom = fom, tom = tom, beløp = BigDecimal(38)),
                            ),
                        ),
                    ),
                ),
            )
        }
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
