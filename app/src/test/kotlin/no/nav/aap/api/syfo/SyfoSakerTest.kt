package no.nav.aap.api.syfo

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
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.aap.api.TestConfig
import no.nav.aap.api.api
import no.nav.aap.api.arena.ArenaService
import no.nav.aap.api.intern.ArenaStatus
import no.nav.aap.api.intern.KelvinStatus
import no.nav.aap.api.intern.SyfoSakerRequest
import no.nav.aap.api.intern.SyfoSakerResponse
import no.nav.aap.api.intern.SyfoSak
import no.nav.aap.api.intern.SyfoVedtak
import no.nav.aap.api.intern.behandlingsflyt.Periode as KelvinPeriode
import no.nav.aap.api.intern.Periode as SakStatusPeriode
import no.nav.aap.api.intern.behandlingsflyt.SakStatus as KelvinSakStatus
import no.nav.aap.api.intern.behandlingsflyt.SakstatusFraKelvin
import no.nav.aap.api.kelvin.Behandling
import no.nav.aap.api.kelvin.KelvinBehandlingStatus
import no.nav.aap.api.kelvin.RettighetsTypePeriode
import no.nav.aap.api.kelvin.Sak
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.api.postgres.SakStatusRepository
import no.nav.aap.api.util.AzureTokenGen
import no.nav.aap.api.util.FakeArenaGateway
import no.nav.aap.api.util.Fakes
import no.nav.aap.api.util.PdlGatewayEmpty
import no.nav.aap.api.util.PostgresTestBase
import no.nav.aap.api.util.WithFakes
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus as ArenaSakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.Status as ArenaStatusKontrakt
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum as ArenaMaksimum
import no.nav.aap.arenaoppslag.kontrakt.modeller.Periode as ArenaPeriode
import no.nav.aap.arenaoppslag.kontrakt.modeller.Vedtak as ArenaVedtak
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@WithFakes
class SyfoSakerTest : PostgresTestBase() {
    private val personidentifikator = "12345678910"
    private val kelvinSakId = "KELVIN-SAK"
    private val arenaSakId = "ARENA-SAK"
    private val fom = LocalDate.of(2025, 1, 1)
    private val tom = LocalDate.of(2025, 12, 31)

    @Test
    fun `returnerer soknader og vedtak fra Arena og Kelvin`() {
        lagreKelvinData(vedtaksdato = fom.minusDays(3), vedtakId = 122L)
        lagreKelvinData()

        testApplication {
            application {
                api(
                    config = TestConfig.default(),
                    datasourceFactory = { dataSource },
                    arenaService = arenaServiceMedVedtak(),
                    pdlGateway = PdlGatewayEmpty(),
                    modiaProducer = Fakes.getKafka(),
                    aapHendelseProducer = Fakes.getAapHendelse(),
                )
            }

            val response = jsonHttpClient.post("/syfo/sakerByFnr") {
                bearerAuth(
                    AzureTokenGen("test", "test").generate(
                        isApp = false,
                        azp = System.getProperty("AZP_SYFOPERSON"),
                    )
                )
                contentType(ContentType.Application.Json)
                setBody(SyfoSakerRequest(personidentifikator))
            }

            assertThat(response.body<SyfoSakerResponse>()).isEqualTo(
                SyfoSakerResponse(
                    saker = listOf(
                        SyfoSak.Kelvin(
                            sakid = kelvinSakId,
                            statuskode = KelvinStatus.FERDIGBEHANDLET,
                            soknadsdatoer = listOf(fom.minusMonths(1)),
                            vedtak = listOf(
                                SyfoVedtak(
                                    vedtaksdato = fom.minusDays(2),
                                    perioder = listOf(SakStatusPeriode(fom, tom)),
                                ),
                            ),
                        ),
                        SyfoSak.Arena(
                            sakid = arenaSakId,
                            statuskode = ArenaStatus.IVERK,
                            vedtak = listOf(
                                SyfoVedtak(
                                    vedtaksdato = fom.minusDays(1),
                                    perioder = listOf(SakStatusPeriode(fom, tom)),
                                ),
                            ),
                        ),
                    ),
                ),
            )
        }
    }

    @Test
    fun `returnerer tom saksliste ved manglende data`() {
        testApplication {
            application {
                api(
                    config = TestConfig.default(),
                    datasourceFactory = { dataSource },
                    arenaService = Fakes.getArenaService(),
                    pdlGateway = PdlGatewayEmpty(),
                    modiaProducer = Fakes.getKafka(),
                    aapHendelseProducer = Fakes.getAapHendelse(),
                )
            }

            val response = jsonHttpClient.post("/syfo/sakerByFnr") {
                bearerAuth(
                    AzureTokenGen("test", "test").generate(
                        isApp = false,
                        azp = System.getProperty("AZP_SYFOPERSON"),
                    )
                )
                contentType(ContentType.Application.Json)
                setBody(SyfoSakerRequest("10987654321"))
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            val body = response.body<SyfoSakerResponse>()
            assertThat(body.saker).isEmpty()
        }
    }

    @Test
    fun `avviser uautorisert konsument`() {
        testApplication {
            application {
                api(
                    config = TestConfig.default(),
                    datasourceFactory = { dataSource },
                    arenaService = Fakes.getArenaService(),
                    pdlGateway = PdlGatewayEmpty(),
                    modiaProducer = Fakes.getKafka(),
                    aapHendelseProducer = Fakes.getAapHendelse(),
                )
            }

            val response = jsonHttpClient.post("/syfo/sakerByFnr") {
                bearerAuth(
                    AzureTokenGen("test", "test").generate(
                        isApp = false,
                        azp = UUID.randomUUID().toString(),
                    )
                )
                contentType(ContentType.Application.Json)
                setBody(SyfoSakerRequest(personidentifikator))
            }

            assertThat(response.status).isEqualTo(HttpStatusCode.Forbidden)
        }
    }

    private fun lagreKelvinData(
        vedtaksdato: LocalDate = fom.minusDays(2),
        vedtakId: Long = 123L,
    ) {
        dataSource.transaction { connection ->
            SakStatusRepository(connection).lagreSakStatusFraKelvin(
                personidentifikator,
                KelvinSakStatus(
                    sakId = kelvinSakId,
                    søknadsdatoer = listOf(fom.minusMonths(1)),
                    statusKode = SakstatusFraKelvin.FERDIGBEHANDLET,
                    periode = KelvinPeriode(fom, tom),
                ),
            )
            BehandlingsRepository(connection).lagreBehandling(
                listOf(personidentifikator),
                Behandling(
                    behandlingsReferanse = UUID.randomUUID().toString(),
                    rettighetsperiode = Periode(fom, tom),
                    behandlingStatus = KelvinBehandlingStatus.AVSLUTTET,
                    vedtaksDato = vedtaksdato,
                    sak = Sak(kelvinSakId, LocalDateTime.of(fom.minusMonths(2), java.time.LocalTime.NOON)),
                    tilkjent = tidslinjeOf(),
                    rettighetsTypePerioder = listOf(
                        RettighetsTypePeriode(fom, tom, RettighetsType.BISTANDSBEHOV.name)
                    ),
                    samIdOgTpNr = emptyList(),
                    vedtakId = vedtakId,
                    beregningsgrunnlag = BigDecimal.ZERO,
                    nyttVedtak = true,
                    stansOpphørVurdering = emptySet(),
                    arenakompatibleVedtak = emptyList(),
                    foreløpigMaksdato = null,
                    perioderMedFritakMeldeplikt = emptyList(),
                    underveisperioder = emptyList(),
                ),
            )
        }
    }

    private fun arenaServiceMedVedtak(): ArenaService {
        val gateway = FakeArenaGateway(
            saker = listOf(
                ArenaSakStatus(
                    sakId = arenaSakId,
                    statusKode = ArenaStatusKontrakt.IVERK,
                    periode = ArenaPeriode(fom, tom),
                )
            ),
            maksimum = ArenaMaksimum(
                vedtak = listOf(
                    ArenaVedtak(
                        vedtaksId = "arena-vedtak",
                        utbetaling = emptyList(),
                        dagsats = 0,
                        status = "IVERK",
                        utfallkode = "JA",
                        saksnummer = arenaSakId,
                        vedtaksdato = fom.minusDays(1).toString(),
                        vedtaksTypeKode = "O",
                        vedtaksTypeNavn = "Ordinært",
                        periode = ArenaPeriode(fom, tom),
                        rettighetsType = "AAP",
                        beregningsgrunnlag = 0,
                        barnMedStonad = 0,
                        barnetillegg = 0,
                        barnetilleggsats = 0,
                        justertG = null,
                        lopenrvedtak = 1,
                        relatertVedtak = null,
                    )
                )
            ),
        )
        return ArenaService(gateway, gateway)
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
