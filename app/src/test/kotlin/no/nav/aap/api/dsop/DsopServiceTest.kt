package no.nav.aap.api.dsop

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.aap.api.intern.DsopMeldekortDTO
import no.nav.aap.api.intern.DsopRettighetsTypeDTO
import no.nav.aap.api.intern.DsopStatusDTO
import no.nav.aap.api.intern.DsopTimerArbeidetPerDagDTO
import no.nav.aap.api.intern.DsopVedtakDTO
import no.nav.aap.api.intern.DsopVedtaksTypeDTO
import no.nav.aap.api.intern.PeriodeDTO
import no.nav.aap.api.kelvin.Behandling
import no.nav.aap.api.kelvin.KelvinBehandlingStatus
import no.nav.aap.api.kelvin.KelvinSakStatus
import no.nav.aap.api.kelvin.RettighetsTypePeriode
import no.nav.aap.api.kelvin.Sak
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.api.somDTO
import no.nav.aap.api.util.PdlGatewayEmpty
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DsopServiceTest {
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setUp() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() {
        dataSource.close()
    }

    private val fnr = listOf("123445")

    private val testVedtak = Behandling(
        underveisperiode = listOf(),
        rettighetsperiode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 4, 1)),
        behandlingStatus = KelvinBehandlingStatus.UTREDES,
        vedtaksDato = LocalDate.now(),
        sak = Sak(
            saksnummer = "ABCDE",
            status = KelvinSakStatus.OPPRETTET,
            opprettetTidspunkt = LocalDateTime.now()
        ),
        tilkjent = tidslinjeOf(),
        rettighetsTypePerioder = listOf(
            RettighetsTypePeriode(
                LocalDate.of(2021, 1, 1),
                LocalDate.of(2021, 2, 1),
                RettighetsType.BISTANDSBEHOV.name
            ),
            RettighetsTypePeriode(
                LocalDate.of(2021, 2, 2),
                LocalDate.of(2021, 3, 1),
                RettighetsType.SYKEPENGEERSTATNING.name
            ),
            RettighetsTypePeriode(
                LocalDate.of(2021, 3, 2),
                LocalDate.of(2021, 4, 1),
                RettighetsType.SYKEPENGEERSTATNING.name
            ),
        ),
        behandlingsReferanse = UUID.randomUUID().toString(),
        samId = null,
        vedtakId = 1234L,
        beregningsgrunnlag = BigDecimal.ZERO,
        nyttVedtak = false,
        stansOpphørVurdering = emptySet(),
        arenakompatibleVedtak = emptyList(),
    )

    @Test
    fun `lagre ned og hente ut dsop-vedtak, komprimerer like rettighetstypeperioder`() {
        dataSource.transaction {
            BehandlingsRepository(it).lagreBehandling(fnr, testVedtak)
        }

        val res = dataSource.transaction {
            DsopService(it, PdlGatewayEmpty()).hentDsopVedtak(
                "123445",
                Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1))
            )
        }

        assertThat(res)
            .usingRecursiveComparison()
            .isEqualTo(
                listOf(
                    DsopVedtakDTO(
                        vedtakId = "1234",
                        vedtakStatus = DsopStatusDTO.AVSLUTTET,
                        virkningsperiode = PeriodeDTO(
                            LocalDate.of(2021, 1, 1),
                            LocalDate.of(2021, 2, 1)
                        ),
                        rettighetsType = "AAP",
                        utfall = "JA",
                        aktivitetsfase = DsopRettighetsTypeDTO.BISTANDSBEHOV,
                        vedtaksType = DsopVedtaksTypeDTO.E,
                    ),
                    DsopVedtakDTO(
                        vedtakId = "1234",
                        vedtakStatus = DsopStatusDTO.AVSLUTTET,
                        virkningsperiode = PeriodeDTO(
                            LocalDate.of(2021, 2, 2),
                            LocalDate.of(2021, 4, 1)
                        ),
                        rettighetsType = "AAP",
                        utfall = "JA",
                        aktivitetsfase = DsopRettighetsTypeDTO.SYKEPENGEERSTATNING,
                        vedtaksType = DsopVedtaksTypeDTO.E,
                    )
                )
            )
    }

    @Test
    fun `slå sammen meldekort til ett`() {
        val periode = Periode(LocalDate.of(2025, 4, 14), LocalDate.of(2025, 4, 16))
        val meldekort = listOf(
            DsopMeldekortDTO(
                periode = periode.somDTO,
                antallTimerArbeidet = BigDecimal.valueOf(10),
                timerArbeidetPerDag = listOf(
                    DsopTimerArbeidetPerDagDTO(LocalDate.of(2025, 4, 14), 5.0),
                    DsopTimerArbeidetPerDagDTO(LocalDate.of(2025, 4, 15), 5.0)
                ),
                sistOppdatert = LocalDateTime.of(2025, 4, 17, 10, 0)
            ),
            DsopMeldekortDTO(
                periode = periode.somDTO,
                antallTimerArbeidet = BigDecimal.valueOf(5),
                timerArbeidetPerDag = listOf(
                    DsopTimerArbeidetPerDagDTO(LocalDate.of(2025, 4, 15), 2.0),
                    DsopTimerArbeidetPerDagDTO(LocalDate.of(2025, 4, 16), 3.0)
                ),
                sistOppdatert = LocalDateTime.of(2025, 4, 18, 10, 0)
            )
        )

        val res = meldekort.slåSammenMeldeperioder()

        assertThat(res).hasSize(1)
        val sammenslått = res.first()
        assertThat(sammenslått.periode).isEqualTo(periode.somDTO)

        // Meldekort nr 2 er korrigert. Så den 15de er det 2 timer arbeidet. 5+2+3=10
        assertThat(sammenslått.timerArbeidetPerDag.sumOf { it.timerArbeidet }).isEqualByComparingTo(10.0)
        assertThat(sammenslått.timerArbeidetPerDag).containsExactlyInAnyOrder(
            DsopTimerArbeidetPerDagDTO(LocalDate.of(2025, 4, 14), 5.0),
            DsopTimerArbeidetPerDagDTO(LocalDate.of(2025, 4, 15), 2.0),
            DsopTimerArbeidetPerDagDTO(LocalDate.of(2025, 4, 16), 3.0)
        )
        assertThat(sammenslått.sistOppdatert).isEqualTo(LocalDateTime.of(2025, 4, 18, 10, 0))
    }

    @Test
    fun `slå sammen meldekort med ulike perioder`() {
        val periode1 = Periode(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 4, 5))
        val periode2 = Periode(LocalDate.of(2025, 4, 6), LocalDate.of(2025, 4, 10))

        val meldekort = listOf(
            DsopMeldekortDTO(
                periode = periode1.somDTO,
                antallTimerArbeidet = BigDecimal.valueOf(5),
                timerArbeidetPerDag = listOf(DsopTimerArbeidetPerDagDTO(LocalDate.of(2025, 4, 1), 5.0)),
                sistOppdatert = LocalDateTime.now()
            ),
            DsopMeldekortDTO(
                periode = periode2.somDTO,
                antallTimerArbeidet = BigDecimal.valueOf(10),
                timerArbeidetPerDag = listOf(DsopTimerArbeidetPerDagDTO(LocalDate.of(2025, 4, 6), 10.0)),
                sistOppdatert = LocalDateTime.now()
            )
        )

        val res = meldekort.slåSammenMeldeperioder()

        assertThat(res).hasSize(2)
        assertThat(res.map { it.periode }).containsExactlyInAnyOrder(periode1.somDTO, periode2.somDTO)
    }

}