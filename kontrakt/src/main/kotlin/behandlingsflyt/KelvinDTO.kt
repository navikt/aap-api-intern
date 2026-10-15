package no.nav.aap.api.intern.behandlingsflyt

import java.time.LocalDate
import java.time.LocalDateTime

public data class Periode(
    public val fom: LocalDate, public val tom: LocalDate
)

public data class SakStatusKelvin(
    val ident: String,
    val status: SakStatus,
)

public data class OppdaterIdenterDto(
    val saksnummer: String,
    val identer: List<String>,
)

public data class NySøknadDto(
    val personident: String,
)

/**
 * Payload fra behandlingsflyt. Ikke del denne ut.
 */
public data class SakStatus(
    val sakId: String,
    val søknadsdatoer: List<LocalDate>? = emptyList(),
    val statusKode: SakstatusFraKelvin,
    @Deprecated("Ikke del denne ut. Dette er rettighetsperiode, som ikke betyr det vi tror den betyr.")
    val periode: Periode,
)

public enum class SakstatusFraKelvin {
    SOKNAD_UNDER_BEHANDLING,
    REVURDERING_UNDER_BEHANDLING,
    FERDIGBEHANDLET,
}

public enum class BehandlingHendelseNavn{
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
public enum class BehandlingVisningType{
    SØKNAD,
    KLAGE
}
public enum class BehandlingHendelseStatus{
    AKTIV,
    FULLFØRT,
    IKKE_PÅBEGYNT,
}
public data class BehandlingHendelse(
    val navn: BehandlingHendelseNavn,
    val tidsstempel: LocalDateTime,
    val status: BehandlingHendelseStatus,
)

public data class BehandlingVisning(
    val behandlingReferanse: String,
    val avsluttet: Boolean,
    val behandlingType: BehandlingVisningType,
    val hendelser: List<BehandlingHendelse>
)
public data class BehandlingVisningKelvinRequest(
    val ident: String,
    val saksnummer: String,
    val behandling: BehandlingVisning,
)

