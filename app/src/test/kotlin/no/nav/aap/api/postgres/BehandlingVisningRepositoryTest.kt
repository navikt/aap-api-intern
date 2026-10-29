package no.nav.aap.api.postgres

import no.nav.aap.api.mineaap.BehandlingHendelseIntern
import no.nav.aap.api.mineaap.BehandlingHendelseProgresjonIntern
import no.nav.aap.api.mineaap.BehandlingVisningIntern
import no.nav.aap.api.mineaap.BehandlingVisningTypeIntern
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
                BehandlingHendelseIntern.SøknadMottatt(
                    sistEndret = LocalDateTime.of(2025, 1, 2, 10, 15, 30, 123_000_000),
                    progresjon = BehandlingHendelseProgresjonIntern.FULLFØRT,
                ),
                BehandlingHendelseIntern.AlderLovvalgMedlemskap(
                    sistEndret = LocalDateTime.of(2025, 1, 3, 8, 0),
                    progresjon = BehandlingHendelseProgresjonIntern.AKTIV,
                    status = BehandlingHendelseIntern.AlderLovvalgMedlemskap.Status.VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
                ),
                BehandlingHendelseIntern.NavKontor(
                    sistEndret = LocalDateTime.of(2025, 1, 4, 8, 0),
                    progresjon = BehandlingHendelseProgresjonIntern.AKTIV,
                    status = BehandlingHendelseIntern.NavKontor.Status.PURRET_PÅ_LEGERERKLÆRING,
                ),
                BehandlingHendelseIntern.Nay(
                    sistEndret = LocalDateTime.of(2025, 1, 5, 8, 0),
                    progresjon = BehandlingHendelseProgresjonIntern.AKTIV,
                    status = BehandlingHendelseIntern.Nay.Status.MOTTATT_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
                ),
                BehandlingHendelseIntern.FerdigBehandlet(
                    sistEndret = LocalDateTime.of(2025, 1, 6, 8, 0),
                    progresjon = BehandlingHendelseProgresjonIntern.FULLFØRT,
                ),
            ),
        )

        lagre(fnr, "SAK-1", behandling)

        assertThat(hent(fnr)).containsExactly(behandling)
    }

    @Test
    fun `lagrer og henter hendelser uten status og uten sistEndret`() {
        val fnr = "10000000005"
        val behandling = behandlingVisning(
            referanse = "ref-5",
            hendelser = listOf(
                BehandlingHendelseIntern.AlderLovvalgMedlemskap(
                    sistEndret = null,
                    progresjon = BehandlingHendelseProgresjonIntern.IKKE_PÅBEGYNT,
                    status = null,
                ),
                BehandlingHendelseIntern.NavKontor(
                    sistEndret = null,
                    progresjon = BehandlingHendelseProgresjonIntern.IKKE_PÅBEGYNT,
                    status = null,
                ),
                BehandlingHendelseIntern.Nay(
                    sistEndret = null,
                    progresjon = BehandlingHendelseProgresjonIntern.IKKE_PÅBEGYNT,
                    status = null,
                ),
                BehandlingHendelseIntern.FerdigBehandlet(
                    sistEndret = null,
                    progresjon = BehandlingHendelseProgresjonIntern.IKKE_PÅBEGYNT,
                ),
            ),
        )

        lagre(fnr, "SAK-5", behandling)

        assertThat(hent(fnr)).containsExactly(behandling)
    }

    @Test
    fun `ny lagring med samme behandlingReferanse erstatter den gamle`() {
        val fnr = "10000000002"
        val første = behandlingVisning(
            referanse = "ref-2",
            avsluttet = false,
            hendelser = listOf(
                BehandlingHendelseIntern.SøknadMottatt(
                    sistEndret = LocalDateTime.of(2025, 1, 2, 10, 0),
                    progresjon = BehandlingHendelseProgresjonIntern.AKTIV,
                ),
            ),
        )
        val oppdatert = første.copy(
            avsluttet = true,
            hendelser = listOf(
                BehandlingHendelseIntern.FerdigBehandlet(
                    sistEndret = LocalDateTime.of(2025, 2, 1, 12, 0),
                    progresjon = BehandlingHendelseProgresjonIntern.FULLFØRT,
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
        val søknad = behandlingVisning(referanse = "ref-3a", type = BehandlingVisningTypeIntern.SØKNAD)
        val klage = behandlingVisning(referanse = "ref-3b", type = BehandlingVisningTypeIntern.KLAGE)

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

    private fun lagre(fnr: String, saksnummer: String, behandling: BehandlingVisningIntern) {
        dataSource.transaction {
            BehandlingVisningRepository(it).lagreBehandlingVisning(fnr, saksnummer, behandling)
        }
    }

    private fun hent(fnr: String): List<BehandlingVisningIntern> =
        dataSource.transaction { BehandlingVisningRepository(it).hentBehandlingVisninger(fnr) }

    private fun behandlingVisning(
        referanse: String,
        avsluttet: Boolean = false,
        type: BehandlingVisningTypeIntern = BehandlingVisningTypeIntern.SØKNAD,
        hendelser: List<BehandlingHendelseIntern> = listOf(
            BehandlingHendelseIntern.SøknadMottatt(
                sistEndret = LocalDateTime.of(2025, 1, 1, 9, 0),
                progresjon = BehandlingHendelseProgresjonIntern.FULLFØRT,
            ),
        ),
    ) = BehandlingVisningIntern(
        behandlingReferanse = referanse,
        avsluttet = avsluttet,
        behandlingType = type,
        hendelser = hendelser,
    )
}
