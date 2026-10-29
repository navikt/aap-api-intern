package no.nav.aap.api.mineaap

import java.time.LocalDateTime

enum class BehandlingHendelseNavnIntern {
    SØKNAD_MOTTATT,
    ALDER_LOVVALG_MEDLEMSKAP,
    NAV_KONTOR,
    NAY,
    FERDIG_BEHANDLET,
}

enum class BehandlingVisningTypeIntern {
    SØKNAD,
    KLAGE
}

enum class BehandlingHendelseProgresjonIntern {
    AKTIV,
    FULLFØRT,
    IKKE_PÅBEGYNT,
}

public sealed interface BehandlingHendelseIntern {
    public val navn: BehandlingHendelseNavnIntern
    public val sistEndret: LocalDateTime?
    public val progresjon: BehandlingHendelseProgresjonIntern

    public data class SøknadMottatt(
        override val sistEndret: LocalDateTime,
        override val progresjon: BehandlingHendelseProgresjonIntern
    ) : BehandlingHendelseIntern {
        override val navn: BehandlingHendelseNavnIntern = BehandlingHendelseNavnIntern.SØKNAD_MOTTATT
    }

    public data class AlderLovvalgMedlemskap(
        override val sistEndret: LocalDateTime?,
        override val progresjon: BehandlingHendelseProgresjonIntern,
        public val status: Status? = null,
    ) : BehandlingHendelseIntern {
        override val navn: BehandlingHendelseNavnIntern = BehandlingHendelseNavnIntern.ALDER_LOVVALG_MEDLEMSKAP

        public enum class Status {
            VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
            MOTTATT_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
        }
    }

    public data class NavKontor(
        override val sistEndret: LocalDateTime?,
        override val progresjon: BehandlingHendelseProgresjonIntern,
        val status: Status? = null,
    ) : BehandlingHendelseIntern {
        override val navn: BehandlingHendelseNavnIntern = BehandlingHendelseNavnIntern.NAV_KONTOR

        public enum class Status {
            VENTER_PÅ_LEGEERKLÆRING,
            PURRET_PÅ_LEGERERKLÆRING,
            MOTTATT_LEGEERKLÆRING,
        }
    }

    public data class Nay(
        override val sistEndret: LocalDateTime?,
        override val progresjon: BehandlingHendelseProgresjonIntern,
        val status: Status? = null,
    ) : BehandlingHendelseIntern {
        override val navn: BehandlingHendelseNavnIntern = BehandlingHendelseNavnIntern.NAY

        public enum class Status {
            VENTER_PÅ_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
            MOTTATT_INFORMASJON_FRA_UTENLANDSKE_MYNDIGHETER,
        }
    }

    public data class FerdigBehandlet(
        override val sistEndret: LocalDateTime?,
        override val progresjon: BehandlingHendelseProgresjonIntern
    ) : BehandlingHendelseIntern {
        override val navn: BehandlingHendelseNavnIntern = BehandlingHendelseNavnIntern.FERDIG_BEHANDLET
    }
}

data class BehandlingVisningIntern(
    val behandlingReferanse: String,
    val avsluttet: Boolean,
    val behandlingType: BehandlingVisningTypeIntern,
    val hendelser: List<BehandlingHendelseIntern>
)
