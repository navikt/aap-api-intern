package no.nav.aap.api.intern

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.papsign.ktor.openapigen.annotations.properties.description.Description
import java.time.LocalDate
import no.nav.aap.tilgang.plugin.kontrakt.Personreferanse

public data class SyfoSakerRequest(
    val personidentifikator: String,
) : Personreferanse {
    override fun hentPersonreferanse(): String = personidentifikator
}

public data class SyfoSakerResponse(
    val saker: List<SyfoSak>,
)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "kilde",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = SyfoSak.Arena::class, name = "ARENA"),
    JsonSubTypes.Type(value = SyfoSak.Kelvin::class, name = "KELVIN"),
)
public sealed interface SyfoSak {
    public val sakid: String
    public val vedtak: List<SyfoVedtak>
    public val kilde: Kilde

    @JsonTypeName("ARENA")
    public data class Arena(
        override val sakid: String,
        val statuskode: ArenaStatus,
        override val vedtak: List<SyfoVedtak>,
    ) : SyfoSak {
        override val kilde: Kilde = Kilde.ARENA
    }

    @JsonTypeName("KELVIN")
    public data class Kelvin(
        override val sakid: String,
        val statuskode: KelvinStatus,
        @property:Description("Dato for alle søknader på saken. Sortert i stigende rekkefølge.")
        val soknadsdatoer: List<LocalDate>,
        @property:Description("Inneholder alltid nøyaktig ett vedtak, som er sakens nyeste vedtak. Per 24/8 lagres ikke alle vedtaksdatoer ned i api-et.")
        override val vedtak: List<SyfoVedtak>,
    ) : SyfoSak {
        override val kilde: Kilde = Kilde.KELVIN
    }
}

public data class SyfoVedtak(
    val vedtaksdato: LocalDate,
    @property:Description("Perioder med innvilget AAP.")
    val perioder: List<Periode>,
)
