package no.nav.aap.api

import no.nav.aap.api.kelvin.KelvinBehandlingStatus
import no.nav.aap.api.kelvin.KelvinSakStatus
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate


internal class RoutesTest {
    private val nå = LocalDate.of(2025, 6, 1)

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        textBlock = """
# BehandlingStatus | SakStatus | PeriodeFom | PeriodeTom | ForventetStatus
UTREDES                    | UTREDES   | 2021-01-01 | 2023-01-15 | LØPENDE
AVSLUTTET                  | AVSLUTTET | 2021-01-01 | 2023-01-15 | AVSLUTTET
AVSLUTTET                  | UTREDES   | 2021-01-01 | 2023-01-15 | LØPENDE
IVERKSETTES                | UTREDES   | 2021-01-01 | 2026-01-15 | LØPENDE
UTREDES                    | AVSLUTTET | 2021-01-01 | 2025-05-15 | UTREDES
UTREDES                    | UTREDES   | 2021-01-01 | 2025-05-15 | LØPENDE
"""
    )
    fun `Konvertering av vedtakStatus`(
        behandlingStatus: KelvinBehandlingStatus,
        sakStatus: KelvinSakStatus,
        periodeFom: LocalDate,
        periodeTom: LocalDate,
        forventetStatus: String
    ) {

        val res = utledVedtakStatus(
            behandlingStatus, sakStatus,
            Periode(periodeFom, periodeTom),
            nå = nå
        )

        assertThat(res).isEqualTo(
            no.nav.aap.behandlingsflyt.kontrakt.sak.Status.valueOf(
                forventetStatus
            )
        )
    }
}