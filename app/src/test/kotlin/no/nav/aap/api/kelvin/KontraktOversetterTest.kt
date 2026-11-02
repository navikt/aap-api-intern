package no.nav.aap.api.kelvin

import no.nav.aap.api.mineaap.BehandlingHendelseIntern
import no.nav.aap.api.mineaap.BehandlingHendelseProgresjonIntern
import no.nav.aap.api.mineaap.BehandlingVisningIntern
import no.nav.aap.api.mineaap.BehandlingVisningTypeIntern
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BehandlingHendelse
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BehandlingHendelseProgresjon
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BehandlingHendelseType
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.BehandlingVisningDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class KontraktOversetterTest {
    private val tidspunkt = LocalDateTime.of(2025, 1, 2, 10, 15, 30)

    @Test
    fun `oversetter alle hendelsestyper med status`() {
        val referanse = BehandlingReferanse()
        val dto = BehandlingVisningDto(
            behandlingReferanse = referanse,
            avsluttet = true,
            behandlingType = BehandlingHendelseType.SØKNAD,
            hendelser = listOf(
                BehandlingHendelse.SøknadMottatt(tidspunkt, BehandlingHendelseProgresjon.FULLFØRT),
                BehandlingHendelse.AlderLovvalgMedlemskap(
                    tidspunkt,
                    BehandlingHendelseProgresjon.AKTIV,
                    BehandlingHendelse.AlderLovvalgMedlemskap.Status.VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
                ),
                BehandlingHendelse.NavKontor(
                    tidspunkt,
                    BehandlingHendelseProgresjon.AKTIV,
                    BehandlingHendelse.NavKontor.Status.PURRET_PÅ_LEGERERKLÆRING,
                ),
                BehandlingHendelse.Nay(
                    tidspunkt,
                    BehandlingHendelseProgresjon.FULLFØRT,
                    BehandlingHendelse.Nay.Status.MOTTATT_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
                ),
                BehandlingHendelse.FerdigBehandlet(tidspunkt, BehandlingHendelseProgresjon.FULLFØRT),
            ),
        )

        assertThat(dto.tilDomene()).isEqualTo(
            BehandlingVisningIntern(
                behandlingReferanse = referanse.toString(),
                avsluttet = true,
                behandlingType = BehandlingVisningTypeIntern.SØKNAD,
                hendelser = listOf(
                    BehandlingHendelseIntern.SøknadMottatt(tidspunkt, BehandlingHendelseProgresjonIntern.FULLFØRT),
                    BehandlingHendelseIntern.AlderLovvalgMedlemskap(
                        tidspunkt,
                        BehandlingHendelseProgresjonIntern.AKTIV,
                        BehandlingHendelseIntern.AlderLovvalgMedlemskap.Status.VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
                    ),
                    BehandlingHendelseIntern.NavKontor(
                        tidspunkt,
                        BehandlingHendelseProgresjonIntern.AKTIV,
                        BehandlingHendelseIntern.NavKontor.Status.PURRET_PÅ_LEGERERKLÆRING,
                    ),
                    BehandlingHendelseIntern.Nay(
                        tidspunkt,
                        BehandlingHendelseProgresjonIntern.FULLFØRT,
                        BehandlingHendelseIntern.Nay.Status.MOTTATT_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
                    ),
                    BehandlingHendelseIntern.FerdigBehandlet(tidspunkt, BehandlingHendelseProgresjonIntern.FULLFØRT),
                ),
            )
        )
    }

    @Test
    fun `oversetter hendelser uten status og uten sistEndret`() {
        val dto = BehandlingVisningDto(
            behandlingReferanse = BehandlingReferanse(),
            avsluttet = false,
            behandlingType = BehandlingHendelseType.KLAGE,
            hendelser = listOf(
                BehandlingHendelse.AlderLovvalgMedlemskap(null, BehandlingHendelseProgresjon.IKKE_PÅBEGYNT, null),
                BehandlingHendelse.NavKontor(null, BehandlingHendelseProgresjon.IKKE_PÅBEGYNT, null),
                BehandlingHendelse.Nay(null, BehandlingHendelseProgresjon.IKKE_PÅBEGYNT, null),
                BehandlingHendelse.FerdigBehandlet(null, BehandlingHendelseProgresjon.IKKE_PÅBEGYNT),
            ),
        )

        val domene = dto.tilDomene()

        assertThat(domene.behandlingType).isEqualTo(BehandlingVisningTypeIntern.KLAGE)
        assertThat(domene.hendelser).containsExactly(
            BehandlingHendelseIntern.AlderLovvalgMedlemskap(null, BehandlingHendelseProgresjonIntern.IKKE_PÅBEGYNT, null),
            BehandlingHendelseIntern.NavKontor(null, BehandlingHendelseProgresjonIntern.IKKE_PÅBEGYNT, null),
            BehandlingHendelseIntern.Nay(null, BehandlingHendelseProgresjonIntern.IKKE_PÅBEGYNT, null),
            BehandlingHendelseIntern.FerdigBehandlet(null, BehandlingHendelseProgresjonIntern.IKKE_PÅBEGYNT),
        )
    }

    @Test
    fun `oversetter alle statusverdier`() {
        fun alder(s: BehandlingHendelse.AlderLovvalgMedlemskap.Status) =
            BehandlingHendelse.AlderLovvalgMedlemskap(tidspunkt, BehandlingHendelseProgresjon.AKTIV, s).tilDomene()
        fun navKontor(s: BehandlingHendelse.NavKontor.Status) =
            BehandlingHendelse.NavKontor(tidspunkt, BehandlingHendelseProgresjon.AKTIV, s).tilDomene()
        fun nay(s: BehandlingHendelse.Nay.Status) =
            BehandlingHendelse.Nay(tidspunkt, BehandlingHendelseProgresjon.AKTIV, s).tilDomene()

        assertThat(BehandlingHendelse.AlderLovvalgMedlemskap.Status.entries.map { (alder(it) as BehandlingHendelseIntern.AlderLovvalgMedlemskap).status?.name })
            .containsExactlyElementsOf(BehandlingHendelse.AlderLovvalgMedlemskap.Status.entries.map { it.name })
        assertThat(BehandlingHendelse.NavKontor.Status.entries.map { (navKontor(it) as BehandlingHendelseIntern.NavKontor).status?.name })
            .containsExactlyElementsOf(BehandlingHendelse.NavKontor.Status.entries.map { it.name })
        assertThat(BehandlingHendelse.Nay.Status.entries.map { (nay(it) as BehandlingHendelseIntern.Nay).status?.name })
            .containsExactlyElementsOf(BehandlingHendelse.Nay.Status.entries.map { it.name })
    }
}
