package no.nav.aap.api.intern

import com.fasterxml.jackson.annotation.JsonSubTypes
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

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "kilde"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = SakStatus.Arena::class, name = "ARENA"),
    JsonSubTypes.Type(value = SakStatus.Kelvin::class, name = "KELVIN"),
)
@Response(description = "Representerer saker både fra Arena og Kelvin. `enhet` er alltid null fra Arena.")
public sealed interface SakStatus {
    public val sakId: String
    public val periode: Periode
    public val statusKode: SakStatusEnum

    public val kilde: Kilde

    @JsonTypeName("ARENA")
    public data class Arena(
        override val statusKode: ArenaStatus,
        override val periode: Periode,
        override val sakId: String,
    ) : SakStatus {
        override val kilde: Kilde = Kilde.ARENA

        public fun periode(): Periode = this.periode
    }

    public enum class YtelseStatus {
        FOR_VEDTAK, LOPENDE, AVSLUTTET
    }

    @JsonTypeName("KELVIN")
    public data class Kelvin(
        override val statusKode: KelvinStatus,
        override val periode: Periode,
        override val sakId: String,
        public val ytelsestatus: YtelseStatus,
        public val perioder: List<Periode>,
        public val enhet: NåværendeEnhet? = null
    ) : SakStatus {
        override val kilde: Kilde = Kilde.KELVIN

        public fun status(): KelvinStatus = this.statusKode
    }
}

@Response(description = "Representerer saker fra Kelvin.")
public data class SakStatusOverlappskontroll(
    @Deprecated("Ikke i bruk av konsument.")
    val sakId: String,
    @Deprecated("Ikke i bruk av konsument.")
    val statusKode: KelvinStatus,
    @Deprecated("Ikke i bruk av konsument (straks).")
    val periode: Periode,
    val fraDato: LocalDate,
    val kilde: Kilde,
    @Deprecated("Ikke i bruk av konsument.")
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

public sealed interface SakStatusEnum

public enum class ArenaStatus : SakStatusEnum {
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

public enum class KelvinStatus : SakStatusEnum {
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