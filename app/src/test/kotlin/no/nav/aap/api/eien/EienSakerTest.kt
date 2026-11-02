package no.nav.aap.api.eien

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import no.nav.aap.api.TestConfig
import no.nav.aap.api.api
import no.nav.aap.api.intern.PeriodeDTO
import no.nav.aap.api.intern.EienSak
import no.nav.aap.api.intern.EienSakerRequest
import no.nav.aap.api.intern.EienSakerResponse
import no.nav.aap.api.intern.EienSaksbehandlingsstatus
import no.nav.aap.api.intern.EienYtelsesstatus
import no.nav.aap.api.intern.Kilde
import no.nav.aap.api.intern.behandlingsflyt.Periode as KelvinPeriode
import no.nav.aap.api.intern.behandlingsflyt.SakStatus as KelvinSakStatus
import no.nav.aap.api.intern.behandlingsflyt.SakstatusFraKelvin
import no.nav.aap.api.kelvin.Behandling
import no.nav.aap.api.kelvin.KelvinBehandlingStatus
import no.nav.aap.api.kelvin.RettighetsTypePeriode
import no.nav.aap.api.kelvin.Sak
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.api.postgres.SakStatusRepository
import no.nav.aap.api.util.AzureTokenGen
import no.nav.aap.api.util.Fakes
import no.nav.aap.api.util.PdlGatewayEmpty
import no.nav.aap.api.util.PostgresTestBase
import no.nav.aap.api.util.nyIdent
import no.nav.aap.api.util.WithFakes
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.oppgave.enhet.NåværendeEnhet
import no.nav.aap.oppgave.enhet.OppgaveKategori
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@WithFakes
class EienSakerTest : PostgresTestBase() {
    private val personidentifikator = "12345678910"
    private val sakId = "KELVIN-SAK"
    private val søknadsdato = LocalDate.of(2024, 12, 1)
    private val vedtaksdato = LocalDate.of(2024, 12, 20)
    private val maksdato = LocalDate.of(2027, 12, 31)

    @Test
    fun `returnerer Kelvin-sak med komprimerte perioder med rett`() {
        lagreSakStatus(personidentifikator, sakId, SakstatusFraKelvin.FERDIGBEHANDLET)
        lagreBehandling(
            personidentifikator,
            sakId,
            rettighetsTypePerioder = listOf(
                RettighetsTypePeriode(
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 6, 30),
                    RettighetsType.BISTANDSBEHOV.name
                ),
                RettighetsTypePeriode(
                    LocalDate.of(2025, 7, 1),
                    LocalDate.of(2025, 12, 31),
                    RettighetsType.SYKEPENGEERSTATNING.name
                ),
                RettighetsTypePeriode(
                    LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 6, 30),
                    RettighetsType.BISTANDSBEHOV.name
                ),
            ),
        )

        testApplication {
            startApi()

            val response = hentSaker(personidentifikator, eienOboToken())

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.body<EienSakerResponse>().saker).containsExactly(
                EienSak(
                    kilde = Kilde.KELVIN,
                    sakId = sakId,
                    saksbehandlingsstatus = EienSaksbehandlingsstatus.FERDIGBEHANDLET,
                    ytelsesstatus = EienYtelsesstatus.LOPENDE,
                    søknadsdatoer = listOf(søknadsdato),
                    vedtaksdato = vedtaksdato,
                    perioderMedRett = listOf(
                        PeriodeDTO(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)),
                        PeriodeDTO(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30)),
                    ),
                    foreløpigMaksdato = maksdato,
                    venteårsak = null,
                )
            )
        }
    }

    @Test
    fun `returnerer venteårsak for sak under behandling`() {
        val ident = nyIdent()
        val saksnummer = "SAK-PÅ-VENT"
        val venteårsak = "VENTER_PÅ_MEDISINSKE_OPPLYSNINGER"
        lagreSakStatus(ident, saksnummer, SakstatusFraKelvin.SOKNAD_UNDER_BEHANDLING)
        Fakes.leggTilOppgaveEnhet(
            ident,
            NåværendeEnhet(
                oversendtDato = LocalDate.of(2026, 10, 1),
                oppgaveKategori = OppgaveKategori.LOKALKONTOR,
                enhet = "0400",
                markertSomHasteSak = false,
                venteÅrsak = venteårsak,
                saksnummer = saksnummer,
            )
        )

        testApplication {
            startApi()

            val response = hentSaker(ident, eienOboToken())

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.body<EienSakerResponse>().saker).containsExactly(
                EienSak(
                    kilde = Kilde.KELVIN,
                    sakId = saksnummer,
                    saksbehandlingsstatus = EienSaksbehandlingsstatus.SOKNAD_UNDER_BEHANDLING,
                    ytelsesstatus = EienYtelsesstatus.FOR_VEDTAK,
                    søknadsdatoer = listOf(søknadsdato),
                    vedtaksdato = null,
                    perioderMedRett = emptyList(),
                    foreløpigMaksdato = null,
                    venteårsak = venteårsak,
                )
            )
        }
    }

    @Test
    fun `returnerer tom saksliste når personen ikke har saker`() {
        testApplication {
            startApi()

            val response = hentSaker("10987654321", eienOboToken())

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(response.body<EienSakerResponse>().saker).isEmpty()
        }
    }

    @Test
    fun `avviser annen konsument`() {
        testApplication {
            startApi()

            val token = AzureTokenGen("test", "test").generate(
                isApp = false,
                azp = UUID.randomUUID().toString(),
            )
            val response = hentSaker(personidentifikator, token)

            assertThat(response.status).isEqualTo(HttpStatusCode.Forbidden)
        }
    }

    @Test
    fun `avviser maskin-til-maskin-token`() {
        testApplication {
            startApi()

            val token = AzureTokenGen("test", "test").generate(
                isApp = true,
                azp = System.getProperty("AZP_EIEN"),
            )
            val response = hentSaker(personidentifikator, token)

            assertThat(response.status).isEqualTo(HttpStatusCode.Forbidden)
        }
    }

    @Test
    fun `avviser veileder uten tilgang til personen`() {
        val ident = nyIdent()
        lagreSakStatus(ident, sakId, SakstatusFraKelvin.SOKNAD_UNDER_BEHANDLING)

        Fakes.withTilgangNektet {
            testApplication {
                startApi()

                val response = hentSaker(ident, eienOboToken())

                assertThat(response.status).isEqualTo(HttpStatusCode.Forbidden)
            }
        }
    }

    private fun ApplicationTestBuilder.startApi() {
        application {
            api(
                config = TestConfig.default(),
                datasourceFactory = { dataSource },
                arenaService = Fakes.getArenaService(),
                pdlGateway = PdlGatewayEmpty(),
                modiaProducer = Fakes.getKafka(),
                aapHendelseProducer = Fakes.getAapHendelse(),
                arbeidsoppfølgingProducer = Fakes.getArbeidsoppfølgingHendelse()
            )
        }
    }

    private suspend fun ApplicationTestBuilder.hentSaker(ident: String, token: String): HttpResponse =
        jsonHttpClient.post("/eien/sakerByFnr") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(EienSakerRequest(ident))
        }

    private fun eienOboToken(): String =
        AzureTokenGen("test", "test").generate(
            isApp = false,
            azp = System.getProperty("AZP_EIEN"),
        )

    private fun lagreSakStatus(ident: String, sakId: String, status: SakstatusFraKelvin) {
        dataSource.transaction { connection ->
            SakStatusRepository(connection).lagreSakStatusFraKelvin(
                ident,
                KelvinSakStatus(
                    sakId = sakId,
                    søknadsdatoer = listOf(søknadsdato),
                    statusKode = status,
                    periode = KelvinPeriode(søknadsdato, maksdato),
                ),
            )
        }
    }

    private fun lagreBehandling(
        ident: String,
        sakId: String,
        rettighetsTypePerioder: List<RettighetsTypePeriode>,
    ) {
        dataSource.transaction { connection ->
            BehandlingsRepository(connection).lagreBehandling(
                listOf(ident),
                Behandling(
                    behandlingsReferanse = UUID.randomUUID().toString(),
                    rettighetsperiode = Periode(søknadsdato, maksdato),
                    behandlingStatus = KelvinBehandlingStatus.AVSLUTTET,
                    vedtaksDato = vedtaksdato,
                    sak = Sak(sakId, LocalDateTime.of(søknadsdato, LocalTime.NOON)),
                    tilkjent = tidslinjeOf(),
                    rettighetsTypePerioder = rettighetsTypePerioder,
                    samIdOgTpNr = emptyList(),
                    vedtakId = 123L,
                    beregningsgrunnlag = BigDecimal.ZERO,
                    nyttVedtak = true,
                    stansOpphørVurdering = emptySet(),
                    arenakompatibleVedtak = emptyList(),
                    foreløpigMaksdato = maksdato,
                    perioderMedFritakMeldeplikt = emptyList(),
                    underveisperioder = emptyList(),
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
