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
import no.nav.aap.api.kelvin.Behandling
import no.nav.aap.api.kelvin.KelvinBehandlingStatus
import no.nav.aap.api.kelvin.RettighetsTypePeriode
import no.nav.aap.api.kelvin.Sak
import no.nav.aap.api.kelvin.TilkjentYtelse
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.api.util.AzureTokenGen
import no.nav.aap.api.util.Fakes
import no.nav.aap.api.util.PdlGatewayEmpty
import no.nav.aap.api.util.PostgresTestBase
import no.nav.aap.api.util.WithFakes
import no.nav.aap.api.util.localDate
import no.nav.aap.api.util.localDateTime
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.ArbeidIPeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DetaljertMeldekortDTO
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@WithFakes
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
        )
    }

    @Test
    fun `kan lagre og hente meldekort-detaljer`() {
        // Sett inn behandling med tilkjent ytelse slik at utbetalinger-listen populeres
        val tilkjentYtelse = TilkjentYtelse(
            dagsats = 1000,
            effektivDagsats = 1000,
            gradering = 100,
            samordningUføregradering = null,
            grunnlagsfaktor = BigDecimal("3.5"),
            grunnbeløp = BigDecimal("111477"),
            antallBarn = 0,
            barnetilleggsats = BigDecimal.ZERO,
            barnetillegg = BigDecimal.ZERO,
        )
        val behandling = Behandling(
            rettighetsperiode = Periode(testFom, testTom),
            behandlingStatus = KelvinBehandlingStatus.AVSLUTTET,
            vedtaksDato = testFom,
            sak = Sak(saksnummer = "asd123", opprettetTidspunkt = testMottatTidspunkt),
            tilkjent = tidslinjeOf(Periode(testFom, testTom) to tilkjentYtelse),
            rettighetsTypePerioder = listOf(
                RettighetsTypePeriode(
                    testFom,
                    testTom,
                    "SYKEPENGEERSTATNING"
                )
            ),
            behandlingsReferanse = "test-ref-1234",
            samId = null,
            vedtakId = 1234L,
            beregningsgrunnlag = BigDecimal.ZERO,
            nyttVedtak = true,
            stansOpphørVurdering = emptySet(),
            arenakompatibleVedtak = emptyList(),
            foreløpigMaksdato = null,
            perioderMedFritakMeldeplikt = emptyList(),
            underveisperioder = emptyList(),
        )
        dataSource.transaction { connection ->
            BehandlingsRepository(connection).lagreBehandling(listOf("12345678901"), behandling)
        }

        val config = TestConfig.default()
        val azure = AzureTokenGen("test", "test")

        testApplication {
            application {
                api(
                    config = config,
                    datasource = dataSource,
                    arenaService = Fakes.getArenaService(),
                    modiaProducer = Fakes.getKafka(),
                    aapHendelseProducer = Fakes.getAapHendelse(),
                    pdlGateway = PdlGatewayEmpty(),
                )
            }

            val res = jsonHttpClient.post("/api/insert/meldekort-detaljer") {
                bearerAuth(azure.generate(isApp = true))
                contentType(ContentType.Application.Json)
                setBody(listOf(testObject))
            }

            assertEquals(HttpStatusCode.OK, res.status)
            val meldekort = countMeldekort()
            assertThat(meldekort > 0).isTrue

            val meldekortResponse = jsonHttpClient.post("/kelvin/meldekort-detaljer") {
                bearerAuth(azure.generate(isApp = true, azp = System.getProperty("AZP_SAAS_PROXY")))
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

            val detalj = meldekortListe.meldekort.first()
            assertThat(detalj.meldePeriode.fraOgMedDato).isEqualTo(testObject.meldeperiodeFom)
            assertThat(detalj.meldePeriode.tilOgMedDato).isEqualTo(testObject.meldeperiodeTom)
            assertThat(detalj.utbetalinger).isNotEmpty()
            assertThat(detalj.utbetalinger).hasSize(1)
            val førsteUtbetaling = detalj.utbetalinger.first()
            assertThat(førsteUtbetaling.dagsats).isEqualTo(tilkjentYtelse.dagsats)
            assertThat(førsteUtbetaling.utbetalingsgrad).isEqualTo(tilkjentYtelse.gradering)
            assertThat(førsteUtbetaling.fraDato).isEqualTo(testFom)
            assertThat(førsteUtbetaling.tilDato).isEqualTo(testTom) }
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
