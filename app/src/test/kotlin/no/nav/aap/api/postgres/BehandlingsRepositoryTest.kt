package no.nav.aap.api.postgres

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.aap.api.kelvin.Behandling
import no.nav.aap.api.kelvin.GjeldendeStansEllerOpphør
import no.nav.aap.api.kelvin.KelvinBehandlingStatus
import no.nav.aap.api.kelvin.KelvinSakStatus
import no.nav.aap.api.kelvin.RettighetsTypePeriode
import no.nav.aap.api.kelvin.Sak
import no.nav.aap.api.kelvin.StansEllerOpphør
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

    private val fnr = listOf("123445")
    private val testVedtak = Behandling(
        underveisperiode = listOf(),
        rettighetsperiode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 4, 1)),
        behandlingStatus = KelvinBehandlingStatus.UTREDES,
        behandlingsId = "123",
        vedtaksDato = LocalDate.now(),
        sak = Sak(
            saksnummer = "ABCDE",
            status = KelvinSakStatus.OPPRETTET,
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
            BehandlingsRepository(it).lagreBehandling(fnr, testVedtak)
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
    fun `oppdater identer fungerer som forventet`() {
        val saksnummer = "ABC123"
        val originalIdent = "55555555555"

        val behandling = testVedtak.copy(
            sak = Sak(saksnummer, KelvinSakStatus.LØPENDE, testVedtak.sak.opprettetTidspunkt)
        )

        dataSource.transaction {
            val repo = BehandlingsRepository(it)

            repo.lagreBehandling(listOf(originalIdent), behandling)

            val vedtak =
                repo.hentVedtaksData(originalIdent, Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1)))

            assertThat(vedtak.size).isEqualTo(1)
            assertThat(vedtak.single().behandlingsReferanse).isEqualTo(behandling.behandlingsReferanse)
        }

        val nyIdent = "11111111111"

        dataSource.transaction {
            val repo = BehandlingsRepository(it)

            repo.lagreBehandling(listOf(originalIdent), behandling)

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
            GjeldendeStansEllerOpphør(
                fom = LocalDate.of(2021, 6, 1),
                opprettet = Instant.now(),
                vurdering = StansEllerOpphør.STANS,
                avslagsårsaker = emptySet()
            ),
            GjeldendeStansEllerOpphør(
                fom = LocalDate.of(2021, 9, 1),
                opprettet = Instant.now(),
                vurdering = StansEllerOpphør.OPPHØR,
                avslagsårsaker = emptySet()
            ),
        )

        dataSource.transaction {
            BehandlingsRepository(it).lagreBehandling(
                fnr,
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
            GjeldendeStansEllerOpphør(
                fom = LocalDate.of(2021, 6, 1),
                opprettet = Instant.now(),
                vurdering = StansEllerOpphør.STANS,
                avslagsårsaker = emptySet()
            ),
            GjeldendeStansEllerOpphør(
                fom = LocalDate.of(2021, 9, 1),
                opprettet = Instant.now(),
                vurdering = StansEllerOpphør.OPPHØR,
                avslagsårsaker = emptySet()
            ),
        )
        val oppdaterte = setOf(
            GjeldendeStansEllerOpphør(
                fom = LocalDate.of(2021, 11, 1),
                opprettet = Instant.now(),
                vurdering = StansEllerOpphør.STANS,
                avslagsårsaker = emptySet()
            ),
        )

        dataSource.transaction {
            BehandlingsRepository(it).lagreBehandling(
                fnr,
                testVedtak.copy(stansOpphørVurdering = opprinnelige)
            )
        }
        dataSource.transaction {
            BehandlingsRepository(it).lagreBehandling(
                fnr,
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
        assertThat(stansVurderinger.single().vurdering).isEqualTo(StansEllerOpphør.STANS)
    }
}
