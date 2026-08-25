package no.nav.aap.api.intern

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.tilgang.plugin.kontrakt.Personreferanse

public data class DabSakerRequest(
    val personidentifikator: String,
) : Personreferanse {
    override fun hentPersonreferanse(): String = personidentifikator
}

public data class DabSakerResponse(
    val saker: List<DabSak>,
)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "kilde",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = DabSak.Arena::class, name = "ARENA"),
    JsonSubTypes.Type(value = DabSak.Kelvin::class, name = "KELVIN"),
)
public sealed interface DabSak {
    public val sakId: String
    public val kilde: Kilde

    @JsonTypeName("ARENA")
    public data class Arena(
        override val sakId: String,
        val statusKode: ArenaStatus,
        val periode: Periode,
    ) : DabSak {
        override val kilde: Kilde = Kilde.ARENA
    }

    @JsonTypeName("KELVIN")
    public data class Kelvin(
        override val sakId: String,
        val statuskode: KelvinStatus,
        val perioder: List<Periode>,
        val ytelsesstatus: SakStatus.YtelseStatus,
    ) : DabSak {
        override val kilde: Kilde = Kilde.KELVIN
    }
}
