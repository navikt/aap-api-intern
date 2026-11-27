package no.nav.aap.api.mineaap

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

private val osloTidssone = ZoneId.of("Europe/Oslo")

private fun LocalDateTime.medOsloTidssone(): OffsetDateTime =
    atZone(osloTidssone).toOffsetDateTime()

fun BehandlingVisningIntern.tilResponse(): BehandlingVisningResponse =
    BehandlingVisningResponse(
        behandlingReferanse = behandlingReferanse,
        avsluttet = avsluttet,
        behandlingType = when (behandlingType) {
            BehandlingVisningTypeIntern.SØKNAD -> BehandlingVisningTypeResponse.SØKNAD
            BehandlingVisningTypeIntern.KLAGE -> BehandlingVisningTypeResponse.KLAGE
        },
        hendelser = hendelser.map { it.tilResponse() },
    )

private fun BehandlingHendelseProgresjonIntern.tilResponse(): BehandlingHendelseProgresjonResponse =
    when (this) {
        BehandlingHendelseProgresjonIntern.AKTIV -> BehandlingHendelseProgresjonResponse.AKTIV
        BehandlingHendelseProgresjonIntern.FULLFØRT -> BehandlingHendelseProgresjonResponse.FULLFØRT
        BehandlingHendelseProgresjonIntern.IKKE_PÅBEGYNT -> BehandlingHendelseProgresjonResponse.IKKE_PÅBEGYNT
    }

private fun BehandlingHendelseIntern.tilResponse(): BehandlingHendelseResponse =
    when (this) {
        is BehandlingHendelseIntern.SøknadMottatt -> BehandlingHendelseResponse.SøknadMottatt(
            sistEndret = sistEndret.medOsloTidssone(),
            progresjon = progresjon.tilResponse(),
        )

        is BehandlingHendelseIntern.AlderLovvalgMedlemskap -> BehandlingHendelseResponse.AlderLovvalgMedlemskap(
            sistEndret = sistEndret?.medOsloTidssone(),
            progresjon = progresjon.tilResponse(),
            status = when (status) {
                BehandlingHendelseIntern.AlderLovvalgMedlemskap.Status.VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER ->
                    BehandlingHendelseResponse.AlderLovvalgMedlemskap.Status.VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER

                BehandlingHendelseIntern.AlderLovvalgMedlemskap.Status.MOTTATT_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER ->
                    BehandlingHendelseResponse.AlderLovvalgMedlemskap.Status.MOTTATT_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER

                null -> null
            },
        )

        is BehandlingHendelseIntern.NavKontor -> BehandlingHendelseResponse.NavKontor(
            sistEndret = sistEndret?.medOsloTidssone(),
            progresjon = progresjon.tilResponse(),
            status = when (status) {
                BehandlingHendelseIntern.NavKontor.Status.VENTER_PÅ_LEGEERKLÆRING ->
                    BehandlingHendelseResponse.NavKontor.Status.VENTER_PÅ_LEGEERKLÆRING

                BehandlingHendelseIntern.NavKontor.Status.PURRET_PÅ_LEGERERKLÆRING ->
                    BehandlingHendelseResponse.NavKontor.Status.PURRET_PÅ_LEGERERKLÆRING

                BehandlingHendelseIntern.NavKontor.Status.MOTTATT_LEGEERKLÆRING ->
                    BehandlingHendelseResponse.NavKontor.Status.MOTTATT_LEGEERKLÆRING

                BehandlingHendelseIntern.NavKontor.Status.PURRET_PÅ_LEGEERKLÆRING ->
                    BehandlingHendelseResponse.NavKontor.Status.PURRET_PÅ_LEGEERKLÆRING

                null -> null
            },
        )

        is BehandlingHendelseIntern.Nay -> BehandlingHendelseResponse.Nay(
            sistEndret = sistEndret?.medOsloTidssone(),
            progresjon = progresjon.tilResponse(),
            status = when (status) {
                BehandlingHendelseIntern.Nay.Status.VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER ->
                    BehandlingHendelseResponse.Nay.Status.VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER

                BehandlingHendelseIntern.Nay.Status.MOTTATT_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER ->
                    BehandlingHendelseResponse.Nay.Status.MOTTATT_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER

                BehandlingHendelseIntern.Nay.Status.VENTER_PÅ_LEDIG_SAKSBEHANDLER ->
                    BehandlingHendelseResponse.Nay.Status.VENTER_PÅ_LEDIG_SAKSBEHANDLER

                BehandlingHendelseIntern.Nay.Status.UNDER_BEHANDLING ->
                    BehandlingHendelseResponse.Nay.Status.UNDER_BEHANDLING

                null -> null
            },
        )

        is BehandlingHendelseIntern.FerdigBehandlet -> BehandlingHendelseResponse.FerdigBehandlet(
            sistEndret = sistEndret?.medOsloTidssone(),
            progresjon = progresjon.tilResponse(),
        )
    }
