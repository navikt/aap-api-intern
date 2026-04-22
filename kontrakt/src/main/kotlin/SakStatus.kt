package no.nav.aap.api.intern

import com.papsign.ktor.openapigen.annotations.Response
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
@Response(description = "Representerer saker både fra Arena og Kelvin. `enhet` er alltid null fra Arena.")
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

    // Disse skal bort fra Kelvin
    OPPRETTET,
    UTREDES,
    LØPENDE,
    AVSLUTTET,

    // Disse kommer fra Kelvin
    SOKNAD_UNDER_BEHANDLING,
    REVURDERING_UNDER_BEHANDLING,
    FERDIGBEHANDLET,
}