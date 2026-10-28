package no.nav.aap.api.postgres

import no.nav.aap.api.mineaap.BehandlingHendelse
import no.nav.aap.api.mineaap.BehandlingHendelseNavn
import no.nav.aap.api.mineaap.BehandlingHendelseStatus
import no.nav.aap.api.mineaap.BehandlingVisning
import no.nav.aap.api.mineaap.BehandlingVisningType
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BehandlingVisningRepositoryTest {
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
    fun `lagrer og henter behandling med hendelser`() {
        val fnr = "10000000001"
        val behandling = behandlingVisning(
            referanse = "ref-1",
            hendelser = listOf(
                BehandlingHendelse(
                    navn = BehandlingHendelseNavn.SØKNAD_MOTTATT,
                    tidsstempel = LocalDateTime.of(2025, 1, 2, 10, 15, 30, 123_000_000),
                    status = BehandlingHendelseStatus.FULLFØRT,
                ),
                BehandlingHendelse(
                    navn = BehandlingHendelseNavn.UNDER_BEHANDLING,
                    tidsstempel = LocalDateTime.of(2025, 1, 3, 8, 0),
                    status = BehandlingHendelseStatus.AKTIV,
                ),
            ),
        )
        val saksnummer = "SAK-1"

        lagre(fnr, saksnummer, behandling)

        assertThat(hent(fnr, saksnummer)).containsExactly(behandling)
    }

    @Test
    fun `ny lagring med samme behandlingReferanse erstatter den gamle`() {
        val fnr = "10000000002"
        val første = behandlingVisning(
            referanse = "ref-2",
            avsluttet = false,
            hendelser = listOf(
                BehandlingHendelse(
                    navn = BehandlingHendelseNavn.SØKNAD_MOTTATT,
                    tidsstempel = LocalDateTime.of(2025, 1, 2, 10, 0),
                    status = BehandlingHendelseStatus.AKTIV,
                ),
            ),
        )
        val oppdatert = første.copy(
            avsluttet = true,
            hendelser = listOf(
                BehandlingHendelse(
                    navn = BehandlingHendelseNavn.FERDIG_BEHANDLET,
                    tidsstempel = LocalDateTime.of(2025, 2, 1, 12, 0),
                    status = BehandlingHendelseStatus.FULLFØRT,
                ),
            ),
        )
        val saksnummer = "SAK-2"

        lagre(fnr, saksnummer, første)
        lagre(fnr, saksnummer, oppdatert)

        assertThat(hent(fnr, saksnummer)).containsExactly(oppdatert)
    }

    @Test
    fun `ulike behandlingReferanser for samme ident beholdes begge`() {
        val fnr = "10000000003"
        val søknad = behandlingVisning(referanse = "ref-3a", type = BehandlingVisningType.SØKNAD)
        val klage = behandlingVisning(referanse = "ref-3b", type = BehandlingVisningType.KLAGE)
        val saksnummer = "SAK-3"

        lagre(fnr, saksnummer, søknad)
        lagre(fnr, saksnummer, klage)

        assertThat(hent(fnr, saksnummer)).containsExactly(søknad, klage)
    }

    @Test
    fun `lagrer behandling uten hendelser`() {
        val fnr = "10000000004"
        val behandling = behandlingVisning(referanse = "ref-4", hendelser = emptyList())
        val saksnummer = "SAK-4"

        lagre(fnr, saksnummer, behandling)

        assertThat(hent(fnr, saksnummer)).containsExactly(behandling)
    }

    @Test
    fun `returnerer tom liste når ident ikke har behandlinger`() {
        assertThat(hent("19999999999", "sak1")).isEmpty()
    }

    private fun lagre(fnr: String, saksnummer: String, behandling: BehandlingVisning) {
        dataSource.transaction {
            BehandlingVisningRepository(it).lagreBehandlingVisning(fnr, saksnummer, behandling)
        }
    }

    private fun hent(fnr: String, saksnummer: String): List<BehandlingVisning> =
        dataSource.transaction { BehandlingVisningRepository(it).hentBehandlingVisninger(fnr, saksnummer) }

    private fun behandlingVisning(
        referanse: String,
        avsluttet: Boolean = false,
        type: BehandlingVisningType = BehandlingVisningType.SØKNAD,
        hendelser: List<BehandlingHendelse> = listOf(
            BehandlingHendelse(
                navn = BehandlingHendelseNavn.SØKNAD_MOTTATT,
                tidsstempel = LocalDateTime.of(2025, 1, 1, 9, 0),
                status = BehandlingHendelseStatus.FULLFØRT,
            ),
        ),
    ) = BehandlingVisning(
        behandlingReferanse = referanse,
        avsluttet = avsluttet,
        behandlingType = type,
        hendelser = hendelser,
    )
}
