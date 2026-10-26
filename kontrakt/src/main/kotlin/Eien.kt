package no.nav.aap.api.intern

import java.time.LocalDate
import no.nav.aap.tilgang.plugin.kontrakt.Personreferanse

public data class EienSakerRequest(
    val personidentifikator: String,
) : Personreferanse {
    override fun hentPersonreferanse(): String = personidentifikator
}

public data class EienSakerResponse(
    val saker: List<EienSak>,
)

public data class EienSak(
    val kilde: Kilde,
    val sakId: String,
    val saksbehandlingsstatus: EienSaksbehandlingsstatus,
    val ytelsesstatus: EienYtelsesstatus,
    val søknadsdatoer: List<LocalDate>,
    val vedtaksdato: LocalDate?,
    val perioderMedRett: List<PeriodeDTO>,
    val foreløpigMaksdato: LocalDate?,
    val venteårsak: String?,
)

public enum class EienSaksbehandlingsstatus {
    SOKNAD_UNDER_BEHANDLING,
    REVURDERING_UNDER_BEHANDLING,
    FERDIGBEHANDLET,
}

public enum class EienYtelsesstatus {
    FOR_VEDTAK,
    LOPENDE,
    AVSLUTTET,
}
