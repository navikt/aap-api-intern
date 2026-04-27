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
public sealed class SakStatus(
    public val sakId: String,
    public val periode: Periode,
    public val statusKode: SakStatusEnum,
) {

    public abstract val kilde: Kilde

    @JsonTypeName("ARENA")
    public class Arena(
        statusKode: ArenaStatus,
        periode: Periode,
        sakId: String,
    ) : SakStatus(sakId, periode, statusKode) {
        override val kilde: Kilde = Kilde.ARENA

        public fun periode(): Periode = this.periode


    }

    @JsonTypeName("KELVIN")
    public class Kelvin(
        statusKode: KelvinStatus,
        periode: Periode,
        public val perioder: List<Periode>,
        sakId: String,
        public val enhet: NåværendeEnhet? = null
    ) : SakStatus(sakId, periode, statusKode) {
        override val kilde: Kilde = Kilde.KELVIN

        public fun status(): KelvinStatus = this.statusKode as KelvinStatus
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SakStatus

        if (sakId != other.sakId) return false
        if (periode != other.periode) return false
        if (statusKode != other.statusKode) return false
        if (kilde != other.kilde) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sakId.hashCode()
        result = 31 * result + (periode?.hashCode() ?: 0)
        result = 31 * result + statusKode.hashCode()
        result = 31 * result + kilde.hashCode()
        return result
    }

    override fun toString(): String {
        return "SakStatus(periode=$periode, sakId='$sakId', statusKode=$statusKode, kilde=$kilde)"
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