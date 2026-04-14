package no.nav.aap.api.postgres

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BehandlingsRepositoryTest {
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setUp() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() {
        dataSource.close()
    }

    private val testVedtak = DatadelingDTO(
        underveisperiode = listOf(),
        rettighetsPeriodeFom = LocalDate.of(2021, 1, 1),
        rettighetsPeriodeTom = LocalDate.of(2022, 4, 1),
        behandlingStatus = KelvinBehandlingStatus.UTREDES,
        behandlingsId = "123",
        vedtaksDato = LocalDate.now(),
        sak = SakDTO(
            saksnummer = "ABCDE",
            status = KelvinSakStatus.OPPRETTET,
            fnr = listOf("123445"),
            opprettetTidspunkt = LocalDateTime.now()
        ),
        tilkjent = listOf(),
        rettighetsTypeTidsLinje = listOf(
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
        stansOpphørVurdering = emptySet()
    )

    @Test
    fun `lagre og hente ut`() {
        dataSource.transaction {
            BehandlingsRepository(it).lagreBehandling(
                testVedtak
            )
        }

        val uthentetVedtak = dataSource.transaction {
            BehandlingsRepository(it).hentVedtaksData(
                "123445",
                Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1))
            )
        }

        assertThat(uthentetVedtak).hasSize(1)
    }

    @Test
    fun `lagre ned og hente ut dsop-vedtak, komprimerer like rettighetstypeperioder`() {
        dataSource.transaction {
            BehandlingsRepository(it).lagreBehandling(
                testVedtak
            )
        }

        val res = dataSource.transaction {
            BehandlingsRepository(it).hentDsopVedtak(
                "123445",
                Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1))
            )
        }

        assertThat(res)
            .usingRecursiveComparison()
            .isEqualTo(
                listOf(
                    DsopVedtak(
                        vedtakId = "1",
                        vedtakStatus = DsopStatus.AVSLUTTET,
                        virkningsperiode = Periode(
                            LocalDate.of(2021, 1, 1),
                            LocalDate.of(2021, 2, 1)
                        ),
                        rettighetsType = "AAP",
                        utfall = "JA",
                        aktivitetsfase = RettighetsType.BISTANDSBEHOV,
                        vedtaksType = VedtaksType.E,
                    ),
                    DsopVedtak(
                        vedtakId = "1",
                        vedtakStatus = DsopStatus.AVSLUTTET,
                        virkningsperiode = Periode(
                            LocalDate.of(2021, 2, 2),
                            LocalDate.of(2021, 4, 1)
                        ),
                        rettighetsType = "AAP",
                        utfall = "JA",
                        aktivitetsfase = RettighetsType.SYKEPENGEERSTATNING,
                        vedtaksType = VedtaksType.E,
                    )
                )
            )
    }

    @Test
    fun `oppdater identer fungerer som forventet`() {
        val saksnummer = "ABC123"
        val originalIdent = "55555555555"

        val behandling = testVedtak.copy(
            sak = SakDTO(saksnummer, KelvinSakStatus.LØPENDE, listOf(originalIdent))
        )

        dataSource.transaction {
            val repo = BehandlingsRepository(it)

            repo.lagreBehandling(behandling)

            val vedtak =
                repo.hentVedtaksData(originalIdent, Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1)))

            assertThat(vedtak.size).isEqualTo(1)
            assertThat(vedtak.single().behandlingsReferanse).isEqualTo(behandling.behandlingsReferanse)
        }

        val nyIdent = "11111111111"

        dataSource.transaction {
            val repo = BehandlingsRepository(it)

            repo.lagreBehandling(behandling)

            val vedtakFinnesIkke =
                repo.hentVedtaksData(nyIdent, Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1)))

            assertThat(vedtakFinnesIkke).isEmpty()

            repo.lagreOppdaterteIdenter(saksnummer, listOf(originalIdent, nyIdent))

            val oppdatertVedtak =
                repo.hentVedtaksData(nyIdent, Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1)))

            assertThat(oppdatertVedtak.size).isEqualTo(1)
            assertThat(oppdatertVedtak.single().behandlingsReferanse).isEqualTo(behandling.behandlingsReferanse)
        }
    }

    private val søkePeriode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1))

    @Test
    fun `lagre og hente ut stansOpphørVurdering`() {
        val vurderinger = setOf(
            GjeldendeStansEllerOpphørDTO(
                fom = LocalDate.of(2021, 6, 1),
                opprettet = Instant.now(),
                vurdering = StansEllerOpphørEnumDTODomene.STANS,
                avslagsårsaker = emptySet()
            ),
            GjeldendeStansEllerOpphørDTO(
                fom = LocalDate.of(2021, 9, 1),
                opprettet = Instant.now(),
                vurdering = StansEllerOpphørEnumDTODomene.OPPHØR,
                avslagsårsaker = emptySet()
            ),
        )

        dataSource.transaction {
            BehandlingsRepository(it).lagreBehandling(
                testVedtak.copy(stansOpphørVurdering = vurderinger)
            )
        }

        val hentet = dataSource.transaction {
            BehandlingsRepository(it).hentVedtaksData("123445", søkePeriode)
        }

        assertThat(hentet).hasSize(1)
        assertThat(hentet.single().stansOpphørVurdering)
            .usingRecursiveComparison()
            .ignoringFields("opprettet")
            .isEqualTo(vurderinger)
    }

    @Test
    fun `re-lagring av behandling erstatter eksisterende stansOpphørVurdering`() {
        val opprinnelige = setOf(
            GjeldendeStansEllerOpphørDTO(
                fom = LocalDate.of(2021, 6, 1),
                opprettet = Instant.now(),
                vurdering = StansEllerOpphørEnumDTODomene.STANS,
                avslagsårsaker = emptySet()
            ),
            GjeldendeStansEllerOpphørDTO(
                fom = LocalDate.of(2021, 9, 1),
                opprettet = Instant.now(),
                vurdering = StansEllerOpphørEnumDTODomene.OPPHØR,
                avslagsårsaker = emptySet()
            ),
        )
        val oppdaterte = setOf(
            GjeldendeStansEllerOpphørDTO(
                fom = LocalDate.of(2021, 11, 1),
                opprettet = Instant.now(),
                vurdering = StansEllerOpphørEnumDTODomene.STANS,
                avslagsårsaker = emptySet()
            ),
        )

        dataSource.transaction {
            BehandlingsRepository(it).lagreBehandling(
                testVedtak.copy(stansOpphørVurdering = opprinnelige)
            )
        }
        dataSource.transaction {
            BehandlingsRepository(it).lagreBehandling(
                testVedtak.copy(stansOpphørVurdering = oppdaterte)
            )
        }

        val hentet = dataSource.transaction {
            BehandlingsRepository(it).hentVedtaksData("123445", søkePeriode)
        }

        assertThat(hentet).hasSize(1)
        val stansVurderinger = hentet.single().stansOpphørVurdering
        assertThat(stansVurderinger).hasSize(1)
        assertThat(stansVurderinger!!.single().fom).isEqualTo(LocalDate.of(2021, 11, 1))
        assertThat(stansVurderinger.single().vurdering).isEqualTo(StansEllerOpphørEnumDTODomene.STANS)
    }
}
