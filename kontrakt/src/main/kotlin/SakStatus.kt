package no.nav.aap.api.intern

import java.time.LocalDate

public data class Periode(val fraOgMedDato: LocalDate?, val tilOgMedDato: LocalDate?) {
    init {
        if (fraOgMedDato != null && tilOgMedDato != null && fraOgMedDato != tilOgMedDato) {
            require(fraOgMedDato.isBefore(tilOgMedDato)) { "Fra og med-dato må være før til og med-dato: $fraOgMedDato, $tilOgMedDato" }
        }
    }
}

/**
 * @param enhet Kan være null enten om kilde er ARENA, eller om det ikke finnes noen åpne oppgaver for personen.
 */
public data class SakStatus(
    // Burde renames
    val sakId: String,
    val statusKode: Status,
    /**
     * Dette betyr kanskje ikke hva de tror den skal bety.
     */
    val periode: Periode, /// NKS bruker denne
    val kilde: Kilde,
    val enhet: NåværendeEnhet? = null
    // la denne være ikke-null også når det ikke er åpne oppgaver
    // defaulte til lokalkontoret som behandlet søknaden
)

public enum class OppgaveKategori {
    MEDLEMSKAP,
    LOKALKONTOR,
    KVALITETSSIKRING,
    NAY,
    BESLUTTER
}

public data class NåværendeEnhet(
    val oversendtDato: LocalDate,
    val oppgaveKategori: OppgaveKategori,
    val enhet: String,
    )

public data class EnhetOgOversendelse(
    val tilstand: NåværendeEnhet?
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