package no.nav.aap.api.dsop

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import no.nav.aap.api.intern.DsopRettighetsTypeDTO.BISTANDSBEHOV
import no.nav.aap.api.kelvin.Arenavedtak
import no.nav.aap.api.kelvin.Arenavedtak.Vedtaksvariant
import no.nav.aap.api.kelvin.Behandling
import no.nav.aap.api.kelvin.KelvinBehandlingStatus
import no.nav.aap.api.kelvin.RettighetsTypePeriode
import no.nav.aap.api.kelvin.Sak
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class DsopServiceStaticTest {

    @Test
    fun `Innvilgelse etter avslag filtrerer bort avslag`() {
        /* Vi har ikke støtte for avslag, så må filtrere de bort. Test-data er basert på en
        * feil fra produksjon, men med alt identifiserende endret. */
        assertDoesNotThrow {
            DsopService.utledDsopVedtak(
                behandling = Behandling(
                    behandlingsReferanse = UUID.randomUUID().toString(),
                    rettighetsperiode = Periode(LocalDate.parse("2025-10-22"), Tid.MAKS),
                    behandlingStatus = KelvinBehandlingStatus.AVSLUTTET,
                    vedtaksDato = LocalDate.parse("2026-06-29"),
                    sak = Sak(
                        saksnummer = "S1",
                        opprettetTidspunkt = LocalDateTime.parse("2025-10-22T19:33:14.295"),
                    ),
                    tilkjent = Tidslinje.empty(),
                    rettighetsTypePerioder = listOf(
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-01-01"),
                            LocalDate.parse("2026-01-08").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-01-08"),
                            LocalDate.parse("2026-01-09").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-01-09"),
                            LocalDate.parse("2026-01-10").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-01-10"),
                            LocalDate.parse("2026-01-12").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-01-12"),
                            LocalDate.parse("2026-01-16").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-01-16"),
                            LocalDate.parse("2026-01-18").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-01-18"),
                            LocalDate.parse("2026-01-19").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-01-19"),
                            LocalDate.parse("2026-01-22").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-01-22"),
                            LocalDate.parse("2026-01-24").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-01-24"),
                            LocalDate.parse("2026-01-26").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-01-26"),
                            LocalDate.parse("2026-01-27").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-01-27"),
                            LocalDate.parse("2026-01-29").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-01-29"),
                            LocalDate.parse("2026-02-05").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-02-05"),
                            LocalDate.parse("2026-02-06").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-02-06"),
                            LocalDate.parse("2026-02-07").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-02-07"),
                            LocalDate.parse("2026-02-09").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-02-09"),
                            LocalDate.parse("2026-02-13").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-02-13"),
                            LocalDate.parse("2026-02-14").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-02-14"),
                            LocalDate.parse("2026-02-15").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-02-15"),
                            LocalDate.parse("2026-02-16").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-02-16"),
                            LocalDate.parse("2026-02-17").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-02-17"),
                            LocalDate.parse("2026-02-18").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-02-18"),
                            LocalDate.parse("2026-02-19").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-02-19"),
                            LocalDate.parse("2026-02-23").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-02-23"),
                            LocalDate.parse("2026-02-26").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-02-26"),
                            LocalDate.parse("2026-02-27").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-02-27"),
                            LocalDate.parse("2026-02-28").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-02-28"),
                            LocalDate.parse("2026-03-02").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-03-02"),
                            LocalDate.parse("2026-03-03").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-03-03"),
                            LocalDate.parse("2026-03-09").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-03-09"),
                            LocalDate.parse("2026-03-13").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-03-13"),
                            LocalDate.parse("2026-03-14").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-03-14"),
                            LocalDate.parse("2026-03-15").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-03-15"),
                            LocalDate.parse("2026-03-17").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-03-17"),
                            LocalDate.parse("2026-03-23").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-03-23"),
                            LocalDate.parse("2026-03-26").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-03-26"),
                            LocalDate.parse("2026-03-28").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-03-28"),
                            LocalDate.parse("2026-03-31").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-03-31"),
                            LocalDate.parse("2026-04-01").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-04-01"),
                            LocalDate.parse("2026-04-06").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-04-06"),
                            LocalDate.parse("2026-04-08").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-04-08"),
                            LocalDate.parse("2026-04-09").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-04-09"),
                            LocalDate.parse("2026-04-10").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-04-10"),
                            LocalDate.parse("2026-04-13").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-04-13"),
                            LocalDate.parse("2026-04-15").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-04-15"),
                            LocalDate.parse("2026-04-20").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-04-20"),
                            LocalDate.parse("2026-04-21").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-04-21"),
                            LocalDate.parse("2026-04-22").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-04-22"),
                            LocalDate.parse("2026-04-23").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-04-23"),
                            LocalDate.parse("2026-04-27").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-04-27"),
                            LocalDate.parse("2026-04-29").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-04-29"),
                            LocalDate.parse("2026-05-04").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-05-04"),
                            LocalDate.parse("2026-05-07").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-05-07"),
                            LocalDate.parse("2026-05-18").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-05-18"),
                            LocalDate.parse("2026-05-21").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-05-21"),
                            LocalDate.parse("2026-05-26").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-05-26"),
                            LocalDate.parse("2026-05-27").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-05-27"),
                            LocalDate.parse("2026-05-28").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-05-28"),
                            LocalDate.parse("2026-06-01").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-06-01"),
                            LocalDate.parse("2026-06-03").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-06-03"),
                            LocalDate.parse("2026-06-04").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-06-04"),
                            LocalDate.parse("2026-06-08").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-06-08"),
                            LocalDate.parse("2026-06-09").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-06-09"),
                            LocalDate.parse("2026-06-10").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-06-10"),
                            LocalDate.parse("2026-06-15").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-06-15"),
                            LocalDate.parse("2026-06-22").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-06-22"),
                            LocalDate.parse("2026-06-24").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-06-24"),
                            LocalDate.parse("2026-06-29").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-06-29"),
                            LocalDate.parse("2026-07-13").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-07-13"),
                            LocalDate.parse("2026-07-27").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-07-27"),
                            LocalDate.parse("2026-08-10").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-08-10"),
                            LocalDate.parse("2026-08-24").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-08-24"),
                            LocalDate.parse("2026-09-07").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-09-07"),
                            LocalDate.parse("2026-09-21").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-09-21"),
                            LocalDate.parse("2026-10-05").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-10-05"),
                            LocalDate.parse("2026-10-19").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-10-19"),
                            LocalDate.parse("2026-10-22").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-10-22"),
                            LocalDate.parse("2026-11-02").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-11-02"),
                            LocalDate.parse("2026-11-16").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-11-16"),
                            LocalDate.parse("2026-11-30").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-11-30"),
                            LocalDate.parse("2026-12-14").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-12-14"),
                            LocalDate.parse("2026-12-28").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                        RettighetsTypePeriode(
                            LocalDate.parse("2026-12-28"),
                            LocalDate.parse("2027-01-01").minusDays(1),
                            BISTANDSBEHOV.toString()
                        ),
                    ),
                    samId = null,
                    vedtakId = 0L,
                    beregningsgrunnlag = null,
                    nyttVedtak = false,
                    stansOpphørVurdering = null,
                    arenakompatibleVedtak = listOf(
                        Arenavedtak(
                            vedtakId = 3628,
                            fom = LocalDate.parse("2025-10-22"),
                            tom = LocalDate.parse("2025-10-22"),
                            vedtaksvariant = Vedtaksvariant.O_AVSLAG,
                        ),
                        Arenavedtak(
                            vedtakId = 14322,
                            fom = LocalDate.parse("2025-12-15"),
                            tom = LocalDate.parse("2025-12-15"),
                            vedtaksvariant = Vedtaksvariant.O_AVSLAG,
                        ),
                        Arenavedtak(
                            vedtakId = 37072,
                            fom = LocalDate.parse("2026-01-01"),
                            tom = LocalDate.parse("2026-12-31"),
                            vedtaksvariant = Vedtaksvariant.O_INNV_SOKNAD,
                        ),
                    ),
                    foreløpigMaksdato = LocalDate.parse("2029-01-02"),
                    perioderMedFritakMeldeplikt = listOf(),
                    underveisperioder = listOf(),
                ),
                now = LocalDate.parse("2026-07-02"),
            )
        }
    }
}