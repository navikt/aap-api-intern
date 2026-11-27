package no.nav.aap.api.mineaap

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDateTime
import java.time.OffsetDateTime

class MineAapMapperTest {
    private val tidspunkt = LocalDateTime.of(2025, 1, 2, 10, 15)

    @ParameterizedTest
    @CsvSource(
        "2025-01-02T10:15:00, +01:00",
        "2025-07-02T10:15:00, +02:00",
        "2025-10-26T02:30:00, +02:00",
    )
    fun `mapper sistEndret med Oslo-tidssone for alle hendelsestyper`(lokaltTidspunkt: String, offset: String) {
        val sistEndret = LocalDateTime.parse(lokaltTidspunkt)
        val behandling = BehandlingVisningIntern(
            behandlingReferanse = "ref",
            avsluttet = false,
            behandlingType = BehandlingVisningTypeIntern.SØKNAD,
            hendelser = listOf(
                BehandlingHendelseIntern.SøknadMottatt(sistEndret, BehandlingHendelseProgresjonIntern.FULLFØRT),
                BehandlingHendelseIntern.AlderLovvalgMedlemskap(sistEndret, BehandlingHendelseProgresjonIntern.AKTIV),
                BehandlingHendelseIntern.NavKontor(sistEndret, BehandlingHendelseProgresjonIntern.AKTIV),
                BehandlingHendelseIntern.Nay(sistEndret, BehandlingHendelseProgresjonIntern.AKTIV),
                BehandlingHendelseIntern.FerdigBehandlet(sistEndret, BehandlingHendelseProgresjonIntern.FULLFØRT),
            ),
        )

        assertThat(behandling.tilResponse().hendelser).allSatisfy {
            assertThat(it.sistEndret).isEqualTo(OffsetDateTime.parse(lokaltTidspunkt + offset))
        }
    }

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

        val forventetTidspunkt = OffsetDateTime.parse("2025-01-02T10:15:00+01:00")
        assertThat(behandling.tilResponse().hendelser).containsExactly(
            BehandlingHendelseResponse.SøknadMottatt(forventetTidspunkt, BehandlingHendelseProgresjonResponse.FULLFØRT),
            BehandlingHendelseResponse.AlderLovvalgMedlemskap(
                sistEndret = forventetTidspunkt,
                progresjon = BehandlingHendelseProgresjonResponse.AKTIV,
                status = BehandlingHendelseResponse.AlderLovvalgMedlemskap.Status.VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
            ),
            BehandlingHendelseResponse.NavKontor(
                sistEndret = forventetTidspunkt,
                progresjon = BehandlingHendelseProgresjonResponse.AKTIV,
                status = BehandlingHendelseResponse.NavKontor.Status.PURRET_PÅ_LEGERERKLÆRING,
            ),
            BehandlingHendelseResponse.Nay(
                sistEndret = forventetTidspunkt,
                progresjon = BehandlingHendelseProgresjonResponse.AKTIV,
                status = BehandlingHendelseResponse.Nay.Status.MOTTATT_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
            ),
            BehandlingHendelseResponse.FerdigBehandlet(forventetTidspunkt, BehandlingHendelseProgresjonResponse.FULLFØRT),
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
                BehandlingHendelseIntern.FerdigBehandlet(null, BehandlingHendelseProgresjonIntern.IKKE_PÅBEGYNT),
            ),
        )

        assertThat(behandling.tilResponse().hendelser).containsExactly(
            BehandlingHendelseResponse.AlderLovvalgMedlemskap(null, BehandlingHendelseProgresjonResponse.IKKE_PÅBEGYNT),
            BehandlingHendelseResponse.NavKontor(null, BehandlingHendelseProgresjonResponse.IKKE_PÅBEGYNT),
            BehandlingHendelseResponse.Nay(null, BehandlingHendelseProgresjonResponse.IKKE_PÅBEGYNT),
            BehandlingHendelseResponse.FerdigBehandlet(null, BehandlingHendelseProgresjonResponse.IKKE_PÅBEGYNT),
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

    @Test
    fun `mapper alle statusverdier`() {
        fun map(hendelse: BehandlingHendelseIntern) =
            BehandlingVisningIntern("ref", false, BehandlingVisningTypeIntern.SØKNAD, listOf(hendelse))
                .tilResponse().hendelser.single()

        val alder = BehandlingHendelseIntern.AlderLovvalgMedlemskap.Status.entries
        assertThat(alder.map {
            (map(BehandlingHendelseIntern.AlderLovvalgMedlemskap(tidspunkt, BehandlingHendelseProgresjonIntern.AKTIV, it))
                    as BehandlingHendelseResponse.AlderLovvalgMedlemskap).status?.name
        }).containsExactlyElementsOf(alder.map { it.name })

        val navKontor = BehandlingHendelseIntern.NavKontor.Status.entries
        assertThat(navKontor.map {
            (map(BehandlingHendelseIntern.NavKontor(tidspunkt, BehandlingHendelseProgresjonIntern.AKTIV, it))
                    as BehandlingHendelseResponse.NavKontor).status?.name
        }).containsExactlyElementsOf(navKontor.map { it.name })

        val nay = BehandlingHendelseIntern.Nay.Status.entries
        assertThat(nay.map {
            (map(BehandlingHendelseIntern.Nay(tidspunkt, BehandlingHendelseProgresjonIntern.AKTIV, it))
                    as BehandlingHendelseResponse.Nay).status?.name
        }).containsExactlyElementsOf(nay.map { it.name })
    }

}
