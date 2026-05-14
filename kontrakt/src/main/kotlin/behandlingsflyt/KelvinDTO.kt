package no.nav.aap.api.intern.behandlingsflyt

import java.time.LocalDate

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

/**
 * Payload fra behandlingsflyt. Ikke del denne ut.
 */
public data class SakStatus(
    val sakId: String,
    val statusKode: SakstatusFraKelvin,
    @Deprecated("Ikke del denne ut. Dette er rettighetsperiode, som ikke betyr det vi tror den betyr.")
    val periode: Periode,
)

public enum class SakstatusFraKelvin {
    // Kelvin (gammel, skjer ikke lenger)
    UTREDES,

    // Nye:
    SOKNAD_UNDER_BEHANDLING,
    REVURDERING_UNDER_BEHANDLING,
    FERDIGBEHANDLET,
}