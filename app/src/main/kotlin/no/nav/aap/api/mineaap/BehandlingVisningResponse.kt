package no.nav.aap.api.mineaap

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.LocalDateTime

data class BehandlingVisningResponse(
    val behandlingReferanse: String,
    val avsluttet: Boolean,
    val behandlingType: BehandlingVisningTypeResponse,
    val hendelser: List<BehandlingHendelseResponse>,
)

enum class BehandlingVisningTypeResponse {
    SØKNAD,
    KLAGE,
}

enum class BehandlingHendelseNavnResponse {
    SØKNAD_MOTTATT,
    ALDER_LOVVALG_MEDLEMSKAP,
    NAV_KONTOR,
    NAY,
    FERDIG_BEHANDLET,
}

enum class BehandlingHendelseProgresjonResponse {
    AKTIV,
    FULLFØRT,
    IKKE_PÅBEGYNT,
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "navn",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = BehandlingHendelseResponse.SøknadMottatt::class, name = "SØKNAD_MOTTATT"),
    JsonSubTypes.Type(
        value = BehandlingHendelseResponse.AlderLovvalgMedlemskap::class,
        name = "ALDER_LOVVALG_MEDLEMSKAP"
    ),
    JsonSubTypes.Type(value = BehandlingHendelseResponse.NavKontor::class, name = "NAV_KONTOR"),
    JsonSubTypes.Type(value = BehandlingHendelseResponse.Nay::class, name = "NAY"),
    JsonSubTypes.Type(value = BehandlingHendelseResponse.FerdigBehandlet::class, name = "FERDIG_BEHANDLET"),
)
sealed interface BehandlingHendelseResponse {
    val navn: BehandlingHendelseNavnResponse
    val sistEndret: LocalDateTime?
    val progresjon: BehandlingHendelseProgresjonResponse

    @JsonTypeName("SØKNAD_MOTTATT")
    data class SøknadMottatt(
        override val sistEndret: LocalDateTime,
        override val progresjon: BehandlingHendelseProgresjonResponse,
    ) : BehandlingHendelseResponse {
        override val navn: BehandlingHendelseNavnResponse = BehandlingHendelseNavnResponse.SØKNAD_MOTTATT
    }

    @JsonTypeName("ALDER_LOVVALG_MEDLEMSKAP")
    data class AlderLovvalgMedlemskap(
        override val sistEndret: LocalDateTime?,
        override val progresjon: BehandlingHendelseProgresjonResponse,
        val status: Status? = null,
    ) : BehandlingHendelseResponse {
        override val navn: BehandlingHendelseNavnResponse = BehandlingHendelseNavnResponse.ALDER_LOVVALG_MEDLEMSKAP

        enum class Status {
            VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
            MOTTATT_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
        }
    }

    @JsonTypeName("NAV_KONTOR")
    data class NavKontor(
        override val sistEndret: LocalDateTime?,
        override val progresjon: BehandlingHendelseProgresjonResponse,
        val status: Status? = null,
    ) : BehandlingHendelseResponse {
        override val navn: BehandlingHendelseNavnResponse = BehandlingHendelseNavnResponse.NAV_KONTOR

        enum class Status {
            VENTER_PÅ_LEGEERKLÆRING,
            PURRET_PÅ_LEGERERKLÆRING,
            MOTTATT_LEGEERKLÆRING,
        }
    }

    @JsonTypeName("NAY")
    data class Nay(
        override val sistEndret: LocalDateTime?,
        override val progresjon: BehandlingHendelseProgresjonResponse,
        val status: Status? = null,
    ) : BehandlingHendelseResponse {
        override val navn: BehandlingHendelseNavnResponse = BehandlingHendelseNavnResponse.NAY

        enum class Status {
            VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
            MOTTATT_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
        }
    }

    @JsonTypeName("FERDIG_BEHANDLET")
    data class FerdigBehandlet(
        override val sistEndret: LocalDateTime?,
        override val progresjon: BehandlingHendelseProgresjonResponse,
    ) : BehandlingHendelseResponse {
        override val navn: BehandlingHendelseNavnResponse = BehandlingHendelseNavnResponse.FERDIG_BEHANDLET
    }
}
