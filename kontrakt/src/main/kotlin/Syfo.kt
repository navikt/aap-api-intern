package no.nav.aap.api.intern

import java.time.LocalDate
import no.nav.aap.tilgang.plugin.kontrakt.Personreferanse

public data class SyfoSakerRequest(
    val personidentifikator: String,
) : Personreferanse {
    override fun hentPersonreferanse(): String = personidentifikator
}

public data class SyfoSakerResponse(
    val soknader: List<SyfoSoknader>,
    val vedtak: List<SyfoVedtak>,
)

public data class SyfoSoknader(
    val sakId: String,
    val statuskode: KelvinStatus,
    val soknadsdatoer: List<LocalDate>,
) {
    public val kilde: Kilde = Kilde.KELVIN
}

public data class SyfoVedtak(
    val sakId: String,
    val kilde: Kilde,
    val vedtaksdato: LocalDate,
    val perioder: List<Periode>,
)
