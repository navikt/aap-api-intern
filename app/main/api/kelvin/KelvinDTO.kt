package api.kelvin

import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus

data class SakStatusKelvin (
    val ident: String,
    val status: no.nav.aap.api.intern.SakStatus,
)