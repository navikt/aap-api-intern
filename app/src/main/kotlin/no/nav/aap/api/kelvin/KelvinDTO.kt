package no.nav.aap.api.kelvin

import no.nav.aap.api.intern.Kilde
import no.nav.aap.arenaoppslag.kontrakt.intern.Status
import no.nav.aap.komponenter.type.Periode

data class SakStatusKelvin (
    val ident: String,
    val status: SakStatus,
)

data class SakStatus(
    val sakId: String,
    val statusKode: Status,
    val periode: Periode,
    val kilde: Kilde = Kilde.KELVIN
)