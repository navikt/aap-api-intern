package no.nav.aap.api.postgres

import java.time.LocalDate
import no.nav.aap.api.intern.behandlingsflyt.Periode
import no.nav.aap.api.intern.behandlingsflyt.SakStatus
import no.nav.aap.api.intern.behandlingsflyt.SakstatusFraKelvin
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SakStatusRepositoryTest {
    private lateinit var dataSource: TestDataSource

    @BeforeAll
    fun setUp() {
        dataSource = TestDataSource()
    }

    @AfterAll
    fun tearDown() {
        dataSource.close()
    }

    @Test
    fun `lagrer og henter søknadsdatoer`() {
        val fnr = "12345678901"
        val sakStatus = SakStatus(
            sakId = "Sak-1",
            søknadsdatoer = listOf(
                LocalDate.of(2025, 1, 2),
                LocalDate.of(2025, 2, 3),
            ),
            statusKode = SakstatusFraKelvin.FERDIGBEHANDLET,
            periode = Periode(
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 12, 31),
            ),
        )

        dataSource.transaction {
            SakStatusRepository(it).lagreSakStatusFraKelvin(fnr, sakStatus)
        }

        val hentet = dataSource.transaction {
            SakStatusRepository(it).hentSakStatus(fnr)
        }

        assertThat(hentet).hasSize(1)
        assertThat(hentet.single().søknadsdatoer).isEqualTo(sakStatus.søknadsdatoer)
        assertThat(hentet.single().sakId).isEqualTo(sakStatus.sakId)
    }
}
