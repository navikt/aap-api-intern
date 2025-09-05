package no.nav.aap.api.intern

import java.time.LocalDate

public data class MeldekortDetaljerRequest(
    val personidentifikator: String,
    val fraOgMedDato: LocalDate? = null,
)
