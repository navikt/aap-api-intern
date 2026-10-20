package no.nav.aap.api.mineaap

import java.time.LocalDateTime

enum class BehandlingHendelseNavn {
    SØKNAD_MOTTATT,
    UNDER_BEHANDLING,
    FERDIG_BEHANDLET,
    SØKNAD_TRUKKET,
    VENTER_PÅ_MEDISINSKE_OPPLYSNINGER,
    VENTER_PÅ_SVAR_FRA_BRUKER,
    VENTER_PÅ_OPPLYSNINGER,
    VENTER_PÅ_SVAR_PÅ_FORHÅNDSVARSEL,
    VENTER_PÅ_OPPLYSNINGER_FRA_UTENLANDSKE_MYNDIGHETER,
}

enum class BehandlingVisningType {
    SØKNAD,
    KLAGE
}

enum class BehandlingHendelseStatus {
    AKTIV,
    FULLFØRT,
    IKKE_PÅBEGYNT,
}

data class BehandlingHendelse(
    val navn: BehandlingHendelseNavn,
    val tidsstempel: LocalDateTime,
    val status: BehandlingHendelseStatus,
)

data class BehandlingVisning(
    val behandlingReferanse: String,
    val avsluttet: Boolean,
    val behandlingType: BehandlingVisningType,
    val hendelser: List<BehandlingHendelse>
)
