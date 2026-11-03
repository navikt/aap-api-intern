package no.nav.aap.api.mineaap

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class MineAapMapperTest {
    private val tidspunkt = LocalDateTime.of(2025, 1, 2, 10, 15)

    @Test
    fun `mapper alle hendelsestyper`() {
        val behandling = BehandlingVisningIntern(
            behandlingReferanse = "ref",
            avsluttet = false,
            behandlingType = BehandlingVisningTypeIntern.SØKNAD,
            hendelser = listOf(
                BehandlingHendelseIntern.SøknadMottatt(tidspunkt, BehandlingHendelseProgresjonIntern.FULLFØRT),
                BehandlingHendelseIntern.AlderLovvalgMedlemskap(
                    sistEndret = tidspunkt,
                    progresjon = BehandlingHendelseProgresjonIntern.AKTIV,
                    status = BehandlingHendelseIntern.AlderLovvalgMedlemskap.Status.VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
                ),
                BehandlingHendelseIntern.NavKontor(
                    sistEndret = tidspunkt,
                    progresjon = BehandlingHendelseProgresjonIntern.AKTIV,
                    status = BehandlingHendelseIntern.NavKontor.Status.PURRET_PÅ_LEGERERKLÆRING,
                ),
                BehandlingHendelseIntern.Nay(
                    sistEndret = tidspunkt,
                    progresjon = BehandlingHendelseProgresjonIntern.AKTIV,
                    status = BehandlingHendelseIntern.Nay.Status.MOTTATT_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
                ),
                BehandlingHendelseIntern.FerdigBehandlet(tidspunkt, BehandlingHendelseProgresjonIntern.FULLFØRT),
            ),
        )

        assertThat(behandling.tilResponse().hendelser).containsExactly(
            BehandlingHendelseResponse.SøknadMottatt(tidspunkt, BehandlingHendelseProgresjonResponse.FULLFØRT),
            BehandlingHendelseResponse.AlderLovvalgMedlemskap(
                sistEndret = tidspunkt,
                progresjon = BehandlingHendelseProgresjonResponse.AKTIV,
                status = BehandlingHendelseResponse.AlderLovvalgMedlemskap.Status.VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
            ),
            BehandlingHendelseResponse.NavKontor(
                sistEndret = tidspunkt,
                progresjon = BehandlingHendelseProgresjonResponse.AKTIV,
                status = BehandlingHendelseResponse.NavKontor.Status.PURRET_PÅ_LEGERERKLÆRING,
            ),
            BehandlingHendelseResponse.Nay(
                sistEndret = tidspunkt,
                progresjon = BehandlingHendelseProgresjonResponse.AKTIV,
                status = BehandlingHendelseResponse.Nay.Status.MOTTATT_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
            ),
            BehandlingHendelseResponse.FerdigBehandlet(tidspunkt, BehandlingHendelseProgresjonResponse.FULLFØRT),
        )
    }

    @Test
    fun `mapper manglende status og sistEndret til null`() {
        val behandling = BehandlingVisningIntern(
            behandlingReferanse = "ref",
            avsluttet = false,
            behandlingType = BehandlingVisningTypeIntern.SØKNAD,
            hendelser = listOf(
                BehandlingHendelseIntern.AlderLovvalgMedlemskap(null, BehandlingHendelseProgresjonIntern.IKKE_PÅBEGYNT),
                BehandlingHendelseIntern.NavKontor(null, BehandlingHendelseProgresjonIntern.IKKE_PÅBEGYNT),
                BehandlingHendelseIntern.Nay(null, BehandlingHendelseProgresjonIntern.IKKE_PÅBEGYNT),
            ),
        )

        assertThat(behandling.tilResponse().hendelser).containsExactly(
            BehandlingHendelseResponse.AlderLovvalgMedlemskap(null, BehandlingHendelseProgresjonResponse.IKKE_PÅBEGYNT),
            BehandlingHendelseResponse.NavKontor(null, BehandlingHendelseProgresjonResponse.IKKE_PÅBEGYNT),
            BehandlingHendelseResponse.Nay(null, BehandlingHendelseProgresjonResponse.IKKE_PÅBEGYNT),
        )
    }

    @Test
    fun `mapper behandlingType og avsluttet`() {
        BehandlingVisningTypeIntern.entries.forEach { type ->
            val response = BehandlingVisningIntern(
                behandlingReferanse = "ref",
                avsluttet = true,
                behandlingType = type,
                hendelser = emptyList(),
            ).tilResponse()

            assertThat(response.behandlingReferanse).isEqualTo("ref")
            assertThat(response.avsluttet).isTrue()
            assertThat(response.behandlingType.name).isEqualTo(type.name)
        }
    }

}
