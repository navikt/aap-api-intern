package no.nav.aap.api.intern

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.papsign.ktor.openapigen.annotations.Response
import com.papsign.ktor.openapigen.annotations.properties.description.Description
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
//@Response(description = "Representerer saker både fra Arena og Kelvin. `enhet` er alltid null fra Arena.")
//public data class SakStatus(
//    val sakId: String,
//    @property:Description("Fra Kelvin forteller denne om saksbehandlingsstatusen.")
//    val statusKode: Status,
//    val periode: Periode,
//    val kilde: Kilde,
//    val enhet: NåværendeEnhet? = null
//)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kilde")
public sealed class SakStatus(
    public val kilde: Kilde,
    public val sakId: String,
    public val periode: Periode,
    public val statusKode: SakStatus2,
) {

    @JsonTypeName("ARENA")
    public class Arena(
        statusKode: ArenaStatus,
        periode: Periode,
        sakId: String,
    ) : SakStatus(Kilde.ARENA, sakId, periode, statusKode)

    @JsonTypeName("KELVIN")
    public class Kelvin(
        statusKode: KelvinStatus,
        periode: Periode,
        sakId: String,
        public val enhet: NåværendeEnhet? = null
    ) : SakStatus(Kilde.KELVIN, sakId, periode, statusKode)
}

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
    @property:Description("Datoen behandlingen havnet på denne enheten første gang.")
    val oversendtDato: LocalDate,
    @property:Description("Hvilken type oppgave som behandles nå.")
    val oppgaveKategori: OppgaveKategori,
    @property:Description("Firesifret enhetskode.")
    val enhet: String,
)

public enum class Kilde {
    ARENA,
    KELVIN
}

public sealed interface SakStatus2

public enum class ArenaStatus : SakStatus2 {
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
}

public enum class KelvinStatus : SakStatus2 {
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