package no.nav.aap.api.maksimum

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
import no.nav.aap.api.intern.InternVedtakRequestApiIntern
import no.nav.aap.api.intern.KelvinStatus
import no.nav.aap.api.intern.ResponsTilTeamObo
import no.nav.aap.api.intern.behandlingsflyt.Periode
import no.nav.aap.api.intern.behandlingsflyt.SakStatus
import no.nav.aap.api.intern.behandlingsflyt.SakStatusKelvin
import no.nav.aap.api.intern.behandlingsflyt.SakstatusFraKelvin
import no.nav.aap.api.util.AzureTokenGen
import no.nav.aap.api.util.Fakes
import no.nav.aap.api.util.PdlGatewayEmpty
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DatadelingDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.RettighetsTypePeriode
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.SakDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.TilkjentDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class KelvinOboTest {

    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() = dataSource.close()

    private val fnr = "12345678910"
    private val saksnummer = "4MGL8LS"
    private val rettighetsFom = LocalDate.now().minusYears(2)
    private val rettighetsTom = LocalDate.now().minusYears(1)

    private val testVedtak = DatadelingDTO(
        behandlingsId = "111222333",
        behandlingsReferanse = UUID.randomUUID().toString(),
        rettighetsPeriodeFom = rettighetsFom,
        rettighetsPeriodeTom = rettighetsTom,
        behandlingStatus = Status.AVSLUTTET,
        vedtaksDato = rettighetsFom.minusDays(1),
        sak = SakDTO(
            saksnummer = saksnummer,
            fnr = listOf(fnr),
            opprettetTidspunkt = LocalDateTime.now().minusYears(2),
        ),
        tilkjent = listOf(
            TilkjentDTO(
                tilkjentFom = rettighetsFom,
                tilkjentTom = rettighetsTom,
                dagsats = 400,
                gradering = 100,
                grunnlagsfaktor = BigDecimal("2.4"),
                grunnbeløp = BigDecimal("123321"),
                antallBarn = 0,
                barnetilleggsats = BigDecimal.ZERO,
                barnetillegg = BigDecimal.ZERO,
            )
        ),
        rettighetsTypeTidsLinje = listOf(
            RettighetsTypePeriode(
                fom = rettighetsFom,
                tom = rettighetsTom,
                verdi = RettighetsType.BISTANDSBEHOV.name,
            )
        ),
        vedtakId = 99L,
        beregningsgrunnlag = BigDecimal("500000"),
        stansOpphørVurdering = null,
        arenavedtak = emptyList(),
        muligMaksdato = rettighetsTom.plusDays(1),
    )

    private val testSakStatus = SakStatusKelvin(
        ident = fnr,
        status = SakStatus(
            sakId = saksnummer,
            statusKode = SakstatusFraKelvin.FERDIGBEHANDLET,
            periode = Periode(fom = rettighetsFom, tom = rettighetsTom),
        )
    )

    @Test
    fun `kan hente vedtak og sakstatus fra obo-endepunktet`() {
        Fakes().use { fakes ->
            val azure = AzureTokenGen("test", "test")

            testApplication {
                application {
                    api(
                        config = TestConfig.default(),
                        datasource = dataSource,
                        arenaService = fakes.arenaService,
                        pdlGateway = PdlGatewayEmpty(),
                        aapHendelseProducer = fakes.aapHendelse,
                        modiaProducer = fakes.kafka,
                    )
                }

                jsonHttpClient.post("/api/insert/vedtak") {
                    bearerAuth(azure.generate(isApp = true))
                    contentType(ContentType.Application.Json)
                    setBody(testVedtak)
                }.also { assertThat(it.status).isEqualTo(HttpStatusCode.OK) }

                jsonHttpClient.post("/api/insert/sakStatus") {
                    bearerAuth(azure.generate(isApp = true))
                    contentType(ContentType.Application.Json)
                    setBody(testSakStatus)
                }.also { assertThat(it.status).isEqualTo(HttpStatusCode.OK) }

                val oboResponse = jsonHttpClient.post("/kelvin/obo") {
                    bearerAuth(azure.generate(isApp = true, azp = System.getProperty("AZP_VEILARBOPPFOLGING")))
                    contentType(ContentType.Application.Json)
                    setBody(
                        InternVedtakRequestApiIntern(
                            personidentifikator = fnr,
                            fraOgMedDato = rettighetsFom.minusMonths(1),
                            tilOgMedDato = rettighetsTom.plusMonths(1),
                        )
                    )
                }

                assertThat(oboResponse.status).isEqualTo(HttpStatusCode.OK)

                val respons = oboResponse.body<ResponsTilTeamObo>()
                assertThat(respons.sakstatus).isEqualTo(KelvinStatus.FERDIGBEHANDLET)
                assertThat(respons.maksdato).isEqualTo(rettighetsTom.plusDays(1))
                assertThat(respons.vedtak).hasSize(1)
                assertThat(respons.vedtak.first().saksnummer).isEqualTo(saksnummer)
                assertThat(respons.vedtak.first().rettighetsType).isEqualTo(RettighetsType.BISTANDSBEHOV.name)
            }
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
