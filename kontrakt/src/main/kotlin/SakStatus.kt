package no.nav.aap.api.intern

import java.time.LocalDate

public data class Periode(val fraOgMedDato: LocalDate?, val tilOgMedDato: LocalDate?) {
    init {
        if (fraOgMedDato != null && tilOgMedDato != null && fraOgMedDato != tilOgMedDato) {
            require(fraOgMedDato.isBefore(tilOgMedDato)) { "Fra og med-dato må være før til og med-dato: $fraOgMedDato, $tilOgMedDato" }
        }
    }
}

public data class SakStatus(
    val sakId: String,
    val statusKode: Status,
    val periode: Periode,
    val kilde: Kilde = Kilde.ARENA
)

public enum class Kilde {
    ARENA,
    KELVIN
}

public enum class Status {
    AVSLU,
    FORDE,
    GODKJ,
    INNST,
    IVERK,
    KONT,
    MOTAT,
    OPPRE,
    REGIS,
    UKJENT,
    OPPRETTET,
    UTREDES,
    LØPENDE,
    AVSLUTTET
}