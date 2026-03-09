package no.nav.aap.api.kelvin

import no.nav.aap.api.intern.SakStatus
import no.nav.aap.api.postgres.SakStatusRepository

class KelvinSakService(private val sakStatusRepository: SakStatusRepository) {

    /**
     * TODO: lag person-tabell, slik at vi slipper å spørre på hver ident
     */
    fun hentSakStatus(identer: List<String>): List<SakStatus> {
        return identer.flatMap {
            sakStatusRepository.hentSakStatus(it)
        }
    }

    fun hentSakStatus(ident: String): List<SakStatus> {
        return hentSakStatus(listOf(ident))
    }
}