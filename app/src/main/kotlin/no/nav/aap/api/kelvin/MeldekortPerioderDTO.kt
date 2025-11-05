package no.nav.aap.api.kelvin

import no.nav.aap.komponenter.type.Periode

data class MeldekortPerioderDTO (
    val personIdent: String,
    val meldekortPerioder: List<Periode>
)
