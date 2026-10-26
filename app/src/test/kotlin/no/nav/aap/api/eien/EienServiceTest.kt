package no.nav.aap.api.eien

import java.time.LocalDate
import no.nav.aap.api.intern.PeriodeDTO
import no.nav.aap.api.sak.KelvinStatus
import no.nav.aap.api.sak.Periode
import no.nav.aap.api.sak.SakStatus
import no.nav.aap.api.sak.YtelseStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EienServiceTest {

    @Test
    fun `slår sammen perioder som ligger inntil hverandre, men ikke perioder med opphold`() {
        val sak = kelvinSak(
            perioder = listOf(
                Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30)),
                Periode(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 12, 31)),
                Periode(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30)),
            )
        )

        assertThat(sak.tilEienSak().perioderMedRett).containsExactly(
            PeriodeDTO(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)),
            PeriodeDTO(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30)),
        )
    }

    @Test
    fun `ingen perioder gir tom liste`() {
        assertThat(kelvinSak(perioder = emptyList()).tilEienSak().perioderMedRett).isEmpty()
    }

    private fun kelvinSak(perioder: List<Periode>) = SakStatus.Kelvin(
        statusKode = KelvinStatus.FERDIGBEHANDLET,
        periode = Periode(null, null),
        sakId = "SAK",
        ytelsestatus = YtelseStatus.LOPENDE,
        perioder = perioder,
    )
}
