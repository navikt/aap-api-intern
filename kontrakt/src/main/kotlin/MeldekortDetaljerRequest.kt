package no.nav.aap.api.intern

import java.time.LocalDate
import no.nav.aap.tilgang.plugin.kontrakt.Personreferanse

public data class MeldekortDetaljerRequest(
    val personidentifikator: String,
    val fraOgMedDato: LocalDate? = null,
    val tilOgMedDato: LocalDate? = null,
) : Personreferanse {
    override fun hentPersonreferanse(): String = personidentifikator
}
