package no.nav.aap.api.meldekort

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
import no.nav.aap.api.intern.MeldekortDetaljerRequest
import no.nav.aap.api.intern.Meldeplikt
import no.nav.aap.api.intern.NksArbeidsgrad
import no.nav.aap.api.intern.NksDagsats
import no.nav.aap.api.intern.NksDatoperiode
import no.nav.aap.api.intern.NksMeldekortMedTimer
import no.nav.aap.api.intern.NksMeldeperiode
import no.nav.aap.api.intern.NksMeldeperioderResponse
import no.nav.aap.api.intern.NksTimerArbeid
import no.nav.aap.api.intern.ÅrsakTilReduksjon
import no.nav.aap.api.util.AzureTokenGen
import no.nav.aap.api.util.Fakes
import no.nav.aap.api.util.PdlGatewayEmpty
import no.nav.aap.api.util.PostgresTestBase
import no.nav.aap.api.util.WithFakes
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.ArbeidIPeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DatadelingDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DetaljertMeldekortDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.PeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.RettighetsTypePeriode
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.SakDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.TilkjentDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.UnderveisperiodeDatadelingDTO
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
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
class NksMeldeperioderTest : PostgresTestBase() {
    private val personIdent = "12345678901"
    private val saksnummer = "NKS123"
    private val førsteMeldeperiodeFom = LocalDate.of(2026, 6, 15)
    private val førsteMeldeperiodeTom = LocalDate.of(2026, 6, 28)
    private val andreMeldeperiodeFom = LocalDate.of(2026, 6, 29)
    private val andreMeldeperiodeTom = LocalDate.of(2026, 7, 12)

    @Test
    fun `henter nks meldeperioder fra datadeling og detaljerte meldekort`() {
        val azure = AzureTokenGen("test", "test")
        // Klokken settes etter siste meldeperiode for å sikre at all testdata inkluderes
        val clock = Clock.fixed(
            Instant.from(andreMeldeperiodeTom.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)),
            ZoneId.of("UTC")
        )

        testApplication {
            application {
                api(
                    config = TestConfig.default(),
                    datasource = dataSource,
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

            val meldekortResponse = jsonHttpClient.post("/api/insert/meldekort-detaljer") {
                bearerAuth(azure.generate(isApp = true))
                contentType(ContentType.Application.Json)
                setBody(testMeldekort())
            }
            assertThat(meldekortResponse.status).isEqualTo(HttpStatusCode.OK)

            val response = jsonHttpClient.post("/nks/meldeperioder") {
                bearerAuth(azure.generate(isApp = false, azp = System.getProperty("AZP_SAAS_PROXY")))
                contentType(ContentType.Application.Json)
                setBody(
                    MeldekortDetaljerRequest(
                        personidentifikator = personIdent,
                        fraOgMedDato = førsteMeldeperiodeFom.minusDays(1),
                        tilOgMedDato = andreMeldeperiodeTom.plusDays(1),
                    )
                )
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.body<NksMeldeperioderResponse>()

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
    fun `søkeperiode begrenses til i dag selv om tilOgMedDato er i fremtiden`() {
        val azure = AzureTokenGen("test", "test")
        // Klokken settes til første dag i første meldeperiode — før andre meldeperiode starter
        val clock = Clock.fixed(
            Instant.from(førsteMeldeperiodeFom.atStartOfDay().toInstant(ZoneOffset.UTC)),
            ZoneId.of("UTC")
        )

        testApplication {
            application {
                api(
                    config = TestConfig.default(),
                    datasource = dataSource,
                    arenaService = Fakes.getArenaService(),
                    modiaProducer = Fakes.getKafka(),
                    aapHendelseProducer = Fakes.getAapHendelse(),
                    pdlGateway = PdlGatewayEmpty(),
                    clock = clock,
                )
            }

            jsonHttpClient.post("/api/insert/vedtak") {
                bearerAuth(azure.generate(isApp = true))
                contentType(ContentType.Application.Json)
                setBody(testVedtak())
            }

            // Spør med tilOgMedDato langt frem i tid
            val response = jsonHttpClient.post("/nks/meldeperioder") {
                bearerAuth(azure.generate(isApp = false, azp = System.getProperty("AZP_SAAS_PROXY")))
                contentType(ContentType.Application.Json)
                setBody(
                    MeldekortDetaljerRequest(
                        personidentifikator = personIdent,
                        fraOgMedDato = førsteMeldeperiodeFom.minusDays(1),
                        tilOgMedDato = andreMeldeperiodeTom.plusDays(10),
                    )
                )
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.body<NksMeldeperioderResponse>()

            // Kun data frem til klokkedato skal returneres — ikke fremtidige meldeperioder
            assertThat(body.meldeperioder).hasSize(1)
            assertThat(body.meldeperioder.first().fraDato).isEqualTo(førsteMeldeperiodeFom)
        }
    }

    private fun expectedResponse() = NksMeldeperioderResponse(
        meldeperioder = listOf(
            NksMeldeperiode(
                fraDato = førsteMeldeperiodeFom,
                tilDato = førsteMeldeperiodeTom,
                fritakMeldeplikt = listOf(
                    NksDatoperiode(
                        LocalDate.of(2026, 6, 26),
                        LocalDate.of(2026, 6, 28)
                    )
                ),
                meldekortMedTimer = listOf(
                    NksMeldekortMedTimer(
                        "12345",
                        LocalDate.of(2026, 6, 29)
                    )
                ),
                meldekortLevertIMeldeperioden = listOf(
                    NksMeldekortMedTimer(
                        "11111",
                        LocalDate.of(2026, 6, 20)
                    )
                ),
                timerArbeid = listOf(
                    NksTimerArbeid(
                        førsteMeldeperiodeFom,
                        førsteMeldeperiodeTom,
                        BigDecimal("14.5")
                    )
                ),
                arbeidsgrad = NksArbeidsgrad(70, true),
                dagsatser = listOf(
                    NksDagsats(
                        dagsats = 1000,
                        effektivDagsats = 650,
                        gradering = 70,
                        periodeFom = førsteMeldeperiodeFom,
                        periodeTom = førsteMeldeperiodeTom,
                    )
                ),
                meldeplikt = listOf(
                    Meldeplikt(
                        førsteMeldeperiodeFom,
                        førsteMeldeperiodeTom,
                        "MELDT_SEG"
                    )
                ),
                årsakerTilReduksjon = listOf(
                    ÅrsakTilReduksjon.ARBEID_OVER_GRENSEVERDI,
                    ÅrsakTilReduksjon.ARBEID,
                ),
            ),
            NksMeldeperiode(
                fraDato = andreMeldeperiodeFom.plusDays(1),
                tilDato = andreMeldeperiodeTom.minusDays(1),
                fritakMeldeplikt = listOf(
                    NksDatoperiode(
                        LocalDate.of(2026, 7, 5),
                        LocalDate.of(2026, 7, 6)
                    )
                ),
                meldekortMedTimer = emptyList(),
                meldekortLevertIMeldeperioden = emptyList(),
                timerArbeid = listOf(
                    NksTimerArbeid(
                        andreMeldeperiodeFom.plusDays(1),
                        andreMeldeperiodeTom.minusDays(1),
                        BigDecimal.ZERO
                    )
                ),
                arbeidsgrad = NksArbeidsgrad(grad = 20, overGrenseverdi = false),
                dagsatser = listOf(
                    NksDagsats(
                        dagsats = 900,
                        effektivDagsats = 900,
                        gradering = 100,
                        periodeFom = andreMeldeperiodeFom.plusDays(1),
                        periodeTom = andreMeldeperiodeTom.minusDays(1),
                    )
                ),
                meldeplikt = listOf(
                    Meldeplikt(
                        fraDato = andreMeldeperiodeFom.plusDays(1),
                        tilDato = andreMeldeperiodeTom.minusDays(1),
                        status = "IKKE_MELDT_SEG"
                    )
                ),
                årsakerTilReduksjon = listOf(
                    ÅrsakTilReduksjon.BRUDD_PAA_MELDEPLIKT,
                    ÅrsakTilReduksjon.ARBEID,
                ),
            )
        )
    )

    private fun testVedtak() = DatadelingDTO(
        rettighetsPeriodeFom = førsteMeldeperiodeFom,
        rettighetsPeriodeTom = andreMeldeperiodeTom,
        behandlingStatus = Status.AVSLUTTET,
        behandlingsId = "123",
        vedtaksDato = førsteMeldeperiodeFom,
        sak = SakDTO(
            saksnummer = saksnummer,
            fnr = listOf(personIdent),
            opprettetTidspunkt = LocalDateTime.of(2026, 6, 1, 12, 0),
        ),
        tilkjent = listOf(
            TilkjentDTO(
                tilkjentFom = førsteMeldeperiodeFom,
                tilkjentTom = førsteMeldeperiodeTom,
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
            TilkjentDTO(
                tilkjentFom = andreMeldeperiodeFom,
                tilkjentTom = andreMeldeperiodeTom,
                dagsats = 900,
                effektivDagsats = 900,
                gradering = 100,
                samordningUføregradering = null,
                grunnlagsfaktor = BigDecimal("3.5"),
                grunnbeløp = BigDecimal("130000"),
                antallBarn = 1,
                barnetilleggsats = BigDecimal("38"),
                barnetillegg = BigDecimal("38"),
            )
        ),
        rettighetsTypeTidsLinje = listOf(
            RettighetsTypePeriode(
                fom = førsteMeldeperiodeFom,
                tom = andreMeldeperiodeTom,
                verdi = "BISTANDSBEHOV",
            )
        ),
        muligMaksdato = null,
        behandlingsReferanse = "ref-123",
        samId = null,
        vedtakId = 123,
        beregningsgrunnlag = BigDecimal("500000"),
        perioderMedFritakMeldeplikt = listOf(
            PeriodeDTO(
                fom = LocalDate.of(2026, 6, 26),
                tom = LocalDate.of(2026, 6, 28),
            ),
            PeriodeDTO(
                fom = LocalDate.of(2026, 7, 5),
                tom = LocalDate.of(2026, 7, 6),
            )
        ),
        stansOpphørVurdering = null,
        arenavedtak = emptyList(),
        underveisperioder = listOf(
            UnderveisperiodeDatadelingDTO(
                fom = førsteMeldeperiodeFom,
                tom = førsteMeldeperiodeTom,
                meldepliktstatus = "MELDT_SEG",
                arbeidsgrad = 70,
                overgrenseVerdi = true,
                timerArbeidet = BigDecimal("14.5"),
                periode = PeriodeDTO(førsteMeldeperiodeFom, førsteMeldeperiodeTom),
                meldeperiode = PeriodeDTO(førsteMeldeperiodeFom, førsteMeldeperiodeTom),
            ),
            UnderveisperiodeDatadelingDTO(
                fom = andreMeldeperiodeFom.plusDays(1),
                tom = andreMeldeperiodeTom.minusDays(1),
                meldepliktstatus = "IKKE_MELDT_SEG",
                arbeidsgrad = 20,
                overgrenseVerdi = false,
                timerArbeidet = BigDecimal.ZERO,
                periode = PeriodeDTO(
                    andreMeldeperiodeFom.plusDays(1),
                    andreMeldeperiodeTom.minusDays(1)
                ),
                meldeperiode = PeriodeDTO(andreMeldeperiodeFom, andreMeldeperiodeTom)
            )
        ),
    )

    private fun testMeldekort() = listOf(
        DetaljertMeldekortDTO(
            personIdent = personIdent,
            saksnummer = Saksnummer(saksnummer),
            mottattTidspunkt = LocalDateTime.of(2026, 6, 20, 10, 0),
            journalpostId = "11111",
            meldeperiodeFom = LocalDate.of(2026, 5, 1),
            meldeperiodeTom = LocalDate.of(2026, 5, 14),
            behandlingId = 123,
            timerArbeidPerPeriode = listOf(
                ArbeidIPeriodeDTO(
                    periodeFom = LocalDate.of(2026, 5, 1),
                    periodeTom = LocalDate.of(2026, 5, 1),
                    timerArbeidet = BigDecimal(1)
                )
            ),
        ),
        DetaljertMeldekortDTO(
            personIdent = personIdent,
            saksnummer = Saksnummer(saksnummer),
            mottattTidspunkt = LocalDateTime.of(2026, 6, 29, 10, 0),
            journalpostId = "12345",
            meldeperiodeFom = førsteMeldeperiodeFom,
            meldeperiodeTom = førsteMeldeperiodeTom,
            behandlingId = 123,
            timerArbeidPerPeriode = listOf(
                ArbeidIPeriodeDTO(
                    periodeFom = førsteMeldeperiodeFom,
                    periodeTom = førsteMeldeperiodeFom,
                    timerArbeidet = BigDecimal("7.5"),
                )
            ),
        ),
        DetaljertMeldekortDTO(
            personIdent = personIdent,
            saksnummer = Saksnummer(saksnummer),
            mottattTidspunkt = LocalDateTime.of(2026, 7, 13, 10, 0),
            journalpostId = "54321",
            meldeperiodeFom = andreMeldeperiodeFom,
            meldeperiodeTom = andreMeldeperiodeTom,
            behandlingId = 123,
            timerArbeidPerPeriode = listOf(
                ArbeidIPeriodeDTO(
                    periodeFom = andreMeldeperiodeFom,
                    periodeTom = andreMeldeperiodeFom,
                    timerArbeidet = BigDecimal.ZERO,
                )
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
