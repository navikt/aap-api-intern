package no.nav.aap.api.postgres

import no.nav.aap.api.intern.behandlingsflyt.BehandlingHendelse
import no.nav.aap.api.intern.behandlingsflyt.BehandlingHendelseNavn
import no.nav.aap.api.intern.behandlingsflyt.BehandlingHendelseStatus
import no.nav.aap.api.intern.behandlingsflyt.BehandlingVisning
import no.nav.aap.api.intern.behandlingsflyt.BehandlingVisningType
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

        lagre(fnr, "SAK-1", behandling)

        assertThat(hent(fnr)).containsExactly(behandling)
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

        lagre(fnr, "SAK-2", første)
        lagre(fnr, "SAK-2", oppdatert)

        assertThat(hent(fnr)).containsExactly(oppdatert)
    }

    @Test
    fun `ulike behandlingReferanser for samme ident beholdes begge`() {
        val fnr = "10000000003"
        val søknad = behandlingVisning(referanse = "ref-3a", type = BehandlingVisningType.SØKNAD)
        val klage = behandlingVisning(referanse = "ref-3b", type = BehandlingVisningType.KLAGE)

        lagre(fnr, "SAK-3", søknad)
        lagre(fnr, "SAK-3", klage)

        assertThat(hent(fnr)).containsExactly(søknad, klage)
    }

    @Test
    fun `lagrer behandling uten hendelser`() {
        val fnr = "10000000004"
        val behandling = behandlingVisning(referanse = "ref-4", hendelser = emptyList())

        lagre(fnr, "SAK-4", behandling)

        assertThat(hent(fnr)).containsExactly(behandling)
    }

    @Test
    fun `returnerer tom liste når ident ikke har behandlinger`() {
        assertThat(hent("19999999999")).isEmpty()
    }

    private fun lagre(fnr: String, saksnummer: String, behandling: BehandlingVisning) {
        dataSource.transaction {
            BehandlingVisningRepository(it).lagreBehandlingVisning(fnr, saksnummer, behandling)
        }
    }

    private fun hent(fnr: String): List<BehandlingVisning> =
        dataSource.transaction { BehandlingVisningRepository(it).hentBehandlingVisninger(fnr) }

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
