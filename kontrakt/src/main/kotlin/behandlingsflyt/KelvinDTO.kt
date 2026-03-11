package no.nav.aap.api.intern.behandlingsflyt

import java.time.LocalDate

public data class Periode(
    public val fom: LocalDate, public val tom: LocalDate
)

public data class SakStatusKelvin(
    val ident: String,
    val status: SakStatus,
)

/**
 * Payload fra behandlingsflyt. Ikke del denne ut.
 */
public data class SakStatus(
    val sakId: String,
    val statusKode: SakstatusFraKelvin,
    val periode: Periode,
)

public enum class SakstatusFraKelvin {
    // Kelvin:
    OPPRETTET,
    UTREDES,
    LØPENDE,
    AVSLUTTET,

    // Nye:
    SOKNAD_UNDER_BEHANDLING,
    REVURDERING_UNDER_BEHANDLING,
    FERDIGBEHANDLET,
}