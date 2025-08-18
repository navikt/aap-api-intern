package api

import api.postgres.KelvinBehandlingStatus
import api.postgres.KelvinSakStatus
import io.github.nchaugen.tabletest.junit.TableTest
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate


class RoutesTest {
    private val nå = LocalDate.of(2025, 6, 1)

    @TableTest(
        """
          BehandlingStatus | SakStatus | PeriodeFom | PeriodeTom | ForventetStatus
UTREDES                    | UTREDES   | 2021-01-01 | 2023-01-15 | LØPENDE
AVSLUTTET                  | AVSLUTTET | 2021-01-01 | 2023-01-15 | AVSLUTTET
AVSLUTTET                  | UTREDES   | 2021-01-01 | 2023-01-15 | LØPENDE
IVERKSETTES                | UTREDES   | 2021-01-01 | 2026-01-15 | LØPENDE
UTREDES                    | AVSLUTTET | 2021-01-01 | 2025-05-15 | UTREDES
UTREDES                    | UTREDES   | 2021-01-01 | 2025-05-15 | LØPENDE
    """
    )
    fun `fsdf dsf `(
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

        assertThat(res).isEqualTo(forventetStatus)

    }
}