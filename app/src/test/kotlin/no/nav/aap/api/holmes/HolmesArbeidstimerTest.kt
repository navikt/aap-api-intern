package no.nav.aap.api.holmes

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
import no.nav.aap.api.TestConfig
import no.nav.aap.api.api
import no.nav.aap.api.intern.HolmesArbeidstimerResponse
import no.nav.aap.api.intern.HolmesMeldeperiode
import no.nav.aap.api.intern.HolmesTimerArbeid
import no.nav.aap.api.intern.MeldekortDetaljerRequest
import no.nav.aap.api.util.AzureTokenGen
import no.nav.aap.api.util.Fakes
import no.nav.aap.api.util.PdlGatewayEmpty
import no.nav.aap.api.util.PostgresTestBase
import no.nav.aap.api.util.WithFakes
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DatadelingDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.PeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.RettighetsTypePeriode
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.SakDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.TilkjentDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.UnderveisperiodeDatadelingDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@WithFakes
class HolmesArbeidstimerTest : PostgresTestBase() {
    private val personIdent = "12345678901"
    private val saksnummer = "HOLMES123"
    private val meldeperiodeFom = LocalDate.of(2026, 6, 15)
    private val meldeperiodeTom = LocalDate.of(2026, 6, 28)

    @Test
    fun `henter kun arbeidstimer per meldeperiode for holmes`() {
        val azure = AzureTokenGen("test", "test")
        val clock = Clock.fixed(
            Instant.from(meldeperiodeTom.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)),
            ZoneId.of("UTC")
        )

        testApplication {
            application {
                api(
                    config = TestConfig.default(),
                    datasourceFactory = { dataSource },
                    arenaService = Fakes.getArenaService(),
                    modiaProducer = Fakes.getKafka(),
                    aapHendelseProducer = Fakes.getAapHendelse(),
                    pdlGateway = PdlGatewayEmpty(),
                    clock = clock,
                )
            }

            val vedtakResponse = jsonHttpClient.post("/api/insert/vedtak") {
                bearerAuth(azure.generate(isApp = true))
                contentType(ContentType.Application.Json)
                setBody(testVedtak())
            }
            assertThat(vedtakResponse.status).isEqualTo(HttpStatusCode.OK)

            val response = jsonHttpClient.post("/holmes/arbeidstimer") {
                bearerAuth(azure.generate(isApp = false, azp = System.getProperty("AZP_HOLMES_PERSONDATA_API")))
                contentType(ContentType.Application.Json)
                setBody(
                    MeldekortDetaljerRequest(
                        personidentifikator = personIdent,
                        fraOgMedDato = meldeperiodeFom.minusDays(1),
                        tilOgMedDato = meldeperiodeTom.plusDays(1),
                    )
                )
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.body<HolmesArbeidstimerResponse>()

            assertThat(body)
                .usingRecursiveComparison()
                .withComparatorForType(
                    Comparator<BigDecimal> { left, right -> left.compareTo(right) },
                    BigDecimal::class.java
                )
                .isEqualTo(expectedResponse())
        }
    }

    @Test
    fun `avviser kall fra uautorisert konsument`() {
        val azure = AzureTokenGen("test", "test")

        testApplication {
            application {
                api(
                    config = TestConfig.default(),
                    datasourceFactory = { dataSource },
                    arenaService = Fakes.getArenaService(),
                    modiaProducer = Fakes.getKafka(),
                    aapHendelseProducer = Fakes.getAapHendelse(),
                    pdlGateway = PdlGatewayEmpty(),
                )
            }

            val response = jsonHttpClient.post("/holmes/arbeidstimer") {
                bearerAuth(azure.generate(isApp = false, azp = System.getProperty("AZP_SAAS_PROXY")))
                contentType(ContentType.Application.Json)
                setBody(
                    MeldekortDetaljerRequest(
                        personidentifikator = personIdent,
                    )
                )
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.Forbidden)
        }
    }

    private fun expectedResponse() = HolmesArbeidstimerResponse(
        personIdent = personIdent,
        meldeperioder = listOf(
            HolmesMeldeperiode(
                periodeFom = meldeperiodeFom,
                periodeTom = meldeperiodeTom,
                timerArbeid = listOf(
                    HolmesTimerArbeid(
                        meldeperiodeFom,
                        meldeperiodeTom,
                        BigDecimal("14.5")
                    )
                ),
            )
        )
    )

    private fun testVedtak() = DatadelingDTO(
        rettighetsPeriodeFom = meldeperiodeFom,
        rettighetsPeriodeTom = meldeperiodeTom,
        behandlingStatus = Status.AVSLUTTET,
        behandlingsId = "123",
        vedtaksDato = meldeperiodeFom,
        sak = SakDTO(
            saksnummer = saksnummer,
            fnr = listOf(personIdent),
            opprettetTidspunkt = LocalDateTime.of(2026, 6, 1, 12, 0),
        ),
        tilkjent = listOf(
            TilkjentDTO(
                tilkjentFom = meldeperiodeFom,
                tilkjentTom = meldeperiodeTom,
                dagsats = 1000,
                effektivDagsats = 650,
                gradering = 70,
                samordningUføregradering = null,
                grunnlagsfaktor = BigDecimal("3.5"),
                grunnbeløp = BigDecimal("130000"),
                antallBarn = 0,
                barnetilleggsats = BigDecimal.ZERO,
                barnetillegg = BigDecimal.ZERO,
            ),
        ),
        rettighetsTypeTidsLinje = listOf(
            RettighetsTypePeriode(
                fom = meldeperiodeFom,
                tom = meldeperiodeTom,
                verdi = "BISTANDSBEHOV",
            )
        ),
        muligMaksdato = null,
        behandlingsReferanse = "ref-123",
        samIdOgTpr = listOf(),
        vedtakId = 123,
        beregningsgrunnlag = BigDecimal("500000"),
        perioderMedFritakMeldeplikt = listOf(),
        stansOpphørVurdering = null,
        arenavedtak = emptyList(),
        underveisperioder = listOf(
            UnderveisperiodeDatadelingDTO(
                fom = meldeperiodeFom,
                tom = meldeperiodeTom,
                meldepliktstatus = "MELDT_SEG",
                arbeidsgrad = 70,
                overgrenseVerdi = true,
                timerArbeidet = BigDecimal("14.5"),
                periode = PeriodeDTO(meldeperiodeFom, meldeperiodeTom),
                meldeperiode = PeriodeDTO(meldeperiodeFom, meldeperiodeTom),
            ),
        ),
    )

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
