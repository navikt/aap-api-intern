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
    val sakId: String,
    val statusKode: Status,
    val periode: Periode,
    val kilde: Kilde,
    val enhet: NåværendeEnhet? = null
)

public data class SakStatusMeldekortbackend(
    val kilde: Kilde,
    val periode: Periode,
    val sakId: String,
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