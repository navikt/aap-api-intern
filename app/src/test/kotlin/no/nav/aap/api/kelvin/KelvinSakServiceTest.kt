package no.nav.aap.api.kelvin

import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.api.postgres.SakStatusRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullAndEmptySource
import java.time.Instant
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KelvinSakServiceTest {
    private lateinit var dataSource: TestDataSource
    private val nå = LocalDate.of(2026, 11, 5)
    private val rettighetsperiode = Periode(nå.minusDays(30), nå.minusDays(1))

    @BeforeAll
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterAll
    fun tearDown() = dataSource.close()

    @ParameterizedTest
    @NullAndEmptySource
    fun `tom rett-tidslinje er avsluttet uten stans eller opphør`(
        vurderinger: Set<GjeldendeStansEllerOpphør>?
    ) {
        val behandling = behandling(
            rettighetsTypePerioder = emptyList(),
            stansOpphørVurdering = vurderinger,
        )

        assertThat(erAvsluttetNå(behandling, nå)).isTrue()
    }

    @ParameterizedTest
    @EnumSource(StansEllerOpphør::class)
    fun `tom rett-tidslinje er avsluttet selv med fremtidig stans eller opphør`(
        vurdering: StansEllerOpphør
    ) {
        val behandling = behandling(
            rettighetsTypePerioder = emptyList(),
            stansOpphørVurdering = setOf(stansEllerOpphør(nå.plusDays(10), vurdering)),
        )

        assertThat(erAvsluttetNå(behandling, nå)).isTrue()
    }

    @ParameterizedTest
    @NullAndEmptySource
    fun `ikke-tom rett-tidslinje er ikke avsluttet uten stans eller opphør`(
        vurderinger: Set<GjeldendeStansEllerOpphør>?
    ) {
        assertThat(erAvsluttetNå(behandling(stansOpphørVurdering = vurderinger), nå)).isFalse()
    }

    @ParameterizedTest
    @CsvSource("-1, false", "0, false", "1, false", "2, true")
    fun `stans etter siste rett avslutter først når dagen etter stans er passert`(
        dagerEtterStans: Long,
        forventetAvsluttet: Boolean
    ) {
        val behandling = behandling(stansOpphørVurdering = setOf(stansEllerOpphør(nå)))

        assertThat(erAvsluttetNå(behandling, nå.plusDays(dagerEtterStans)))
            .isEqualTo(forventetAvsluttet)
    }

    @Test
    fun `rett etter stans betyr at saken ikke er avsluttet`() {
        val behandling = behandling(
            rettighetsTypePerioder = listOf(
                RettighetsTypePeriode(nå.minusDays(30), nå.minusDays(20), "BISTANDSBEHOV"),
                RettighetsTypePeriode(nå.minusDays(10), nå.plusDays(10), "BISTANDSBEHOV"),
            ),
            stansOpphørVurdering = setOf(stansEllerOpphør(nå.minusDays(15))),
        )

        assertThat(erAvsluttetNå(behandling, nå)).isFalse()
    }

    private fun erAvsluttetNå(behandling: Behandling, dato: LocalDate): Boolean =
        dataSource.transaction { connection ->
            KelvinSakService(SakStatusRepository(connection), BehandlingsRepository(connection))
                .erAvsluttetNå(behandling, dato)
        }

    private fun stansEllerOpphør(
        fom: LocalDate,
        vurdering: StansEllerOpphør = StansEllerOpphør.STANS
    ) = GjeldendeStansEllerOpphør(
        fom = fom,
        opprettet = Instant.parse("2026-11-01T12:00:00Z"),
        vurdering = vurdering,
        avslagsårsaker = emptySet(),
    )

    private fun behandling(
        rettighetsTypePerioder: List<RettighetsTypePeriode> = listOf(
            RettighetsTypePeriode(rettighetsperiode.fom, rettighetsperiode.tom, "BISTANDSBEHOV")
        ),
        stansOpphørVurdering: Set<GjeldendeStansEllerOpphør>? = emptySet()
    ) = Behandling(
        behandlingsReferanse = "00000000-0000-0000-0000-000000000001",
        rettighetsperiode = rettighetsperiode,
        behandlingStatus = KelvinBehandlingStatus.AVSLUTTET,
        vedtaksDato = nå.minusDays(30),
        sak = Sak("S1", nå.minusDays(30).atStartOfDay()),
        tilkjent = Tidslinje.empty(),
        rettighetsTypePerioder = rettighetsTypePerioder,
        samIdOgTpNr = emptyList(),
        vedtakId = 1L,
        beregningsgrunnlag = null,
        nyttVedtak = false,
        stansOpphørVurdering = stansOpphørVurdering,
        arenakompatibleVedtak = emptyList(),
        foreløpigMaksdato = null,
        perioderMedFritakMeldeplikt = emptyList(),
        underveisperioder = emptyList(),
    )
}
