package no.nav.aap.api.dsop

import no.nav.aap.api.intern.*
import no.nav.aap.api.kelvin.*
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.api.somDTO
import no.nav.aap.api.util.PdlGatewayEmpty
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

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
        rettighetsperiode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 4, 1)),
        behandlingStatus = KelvinBehandlingStatus.UTREDES,
        vedtaksDato = LocalDate.now(),
        sak = Sak(
            saksnummer = "ABCDE",
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
                        virkningsperiode = PeriodeNullableTomDTO(
                            LocalDate.of(2021, 1, 1),
                            LocalDate.of(2021, 2, 1)
                        ),
                        utfall = "JA",
                        aktivitetsfase = DsopRettighetsTypeDTO.BISTANDSBEHOV,
                        vedtaksType = DsopVedtaksTypeDTO.E,
                        vedtaksvariant = null,
                    ),
                    DsopVedtakDTO(
                        vedtakId = "1234",
                        vedtakStatus = DsopStatusDTO.AVSLUTTET,
                        virkningsperiode = PeriodeNullableTomDTO(
                            LocalDate.of(2021, 2, 2),
                            LocalDate.of(2021, 4, 1)
                        ),
                        utfall = "JA",
                        aktivitetsfase = DsopRettighetsTypeDTO.SYKEPENGEERSTATNING,
                        vedtaksType = DsopVedtaksTypeDTO.E,
                        vedtaksvariant = null,
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

    @Test
    fun `ordinær førstegangsbehandling`() {
        val vedtaksperiode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 12, 31))
        val x = DsopService.utledDsopVedtak(
            behandling = behandling(
                rettighetstyper = tidslinjeOf(
                    vedtaksperiode to "BISTANDSBEHOV"
                ),
                arenavedtak = listOf(
                    Arenavedtak(
                        vedtakId = 1L,
                        fom = vedtaksperiode.fom,
                        tom = vedtaksperiode.tom,
                        vedtaksvariant = Arenavedtak.Vedtaksvariant.O_INNV_SOKNAD,
                    ),
                ),
            ),
            now = LocalDate.of(2021, 1, 1),
        )
        assertThat(x)
            .isEqualTo(
                listOf(
                    DsopVedtakDTO(
                        vedtakId = "1",
                        vedtakStatus = DsopStatusDTO.LØPENDE,
                        virkningsperiode = PeriodeNullableTomDTO(vedtaksperiode.fom, vedtaksperiode.tom),
                        utfall = "JA",
                        aktivitetsfase = DsopRettighetsTypeDTO.BISTANDSBEHOV,
                        vedtaksType = DsopVedtaksTypeDTO.O,
                        vedtaksvariant = DsopVedtaksvariantDTO.O_INNV_SOKNAD,
                    )
                )
            )
    }

    @Test
    fun `ordinær førstegangsbehandling, men tidlig stans`() {
        val vedtaksperiode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 6, 30))
        val x = DsopService.utledDsopVedtak(
            behandling = behandling(
                rettighetstyper = tidslinjeOf(
                    vedtaksperiode to "BISTANDSBEHOV"
                ),
                arenavedtak = listOf(
                    Arenavedtak(
                        vedtakId = 1L,
                        fom = vedtaksperiode.fom,
                        tom = vedtaksperiode.tom,
                        vedtaksvariant = Arenavedtak.Vedtaksvariant.O_INNV_SOKNAD,
                    ),
                    Arenavedtak(
                        vedtakId = 2L,
                        fom = vedtaksperiode.tom.plusDays(1),
                        tom = vedtaksperiode.tom.plusDays(1),
                        vedtaksvariant = Arenavedtak.Vedtaksvariant.S_STANS,
                    ),
                ),
            ),
            now = LocalDate.of(2020, 7, 1),
        )
        assertThat(x)
            .isEqualTo(
                listOf(
                    DsopVedtakDTO(
                        vedtakId = "1",
                        vedtakStatus = DsopStatusDTO.AVSLUTTET,
                        virkningsperiode = PeriodeNullableTomDTO(vedtaksperiode.fom, vedtaksperiode.tom),
                        utfall = "JA",
                        aktivitetsfase = DsopRettighetsTypeDTO.BISTANDSBEHOV,
                        vedtaksType = DsopVedtaksTypeDTO.O,
                        vedtaksvariant = DsopVedtaksvariantDTO.O_INNV_SOKNAD,
                    ),
                    DsopVedtakDTO(
                        vedtakId = "2",
                        vedtakStatus = DsopStatusDTO.LØPENDE,
                        virkningsperiode = PeriodeNullableTomDTO(vedtaksperiode.tom.plusDays(1), null),
                        utfall = "NEI",
                        aktivitetsfase = null,
                        vedtaksType = DsopVedtaksTypeDTO.S,
                        vedtaksvariant = DsopVedtaksvariantDTO.S_STANS,
                    ),
                )
            )
    }


    private fun behandling(
        rettighetstyper: Tidslinje<String>,
        arenavedtak: List<Arenavedtak>,
        nyttVedtak: Boolean = true,
    ): Behandling {
        return Behandling(
            behandlingsReferanse = UUID.randomUUID().toString(),
            rettighetsperiode = Periode(LocalDate.MIN, LocalDate.MIN),
            behandlingStatus = KelvinBehandlingStatus.AVSLUTTET,
            vedtaksDato = LocalDate.MIN,
            sak = Sak(
                saksnummer = "s1",
                opprettetTidspunkt = LocalDateTime.MIN,
            ),
            tilkjent = Tidslinje.empty(),
            rettighetsTypePerioder = rettighetstyper.map { periode, type ->
                RettighetsTypePeriode(
                    periode.fom,
                    periode.tom,
                    type
                )
            }.verdier().toList(),
            samId = null,
            vedtakId = -1L,
            beregningsgrunnlag = null,
            nyttVedtak = nyttVedtak,
            stansOpphørVurdering = setOf(),
            arenakompatibleVedtak = arenavedtak,
        )
    }
}