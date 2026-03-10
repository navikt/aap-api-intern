package no.nav.aap.api.kelvin

import no.nav.aap.api.intern.NåværendeEnhet
import no.nav.aap.api.intern.SakStatus
import no.nav.aap.api.postgres.SakStatusRepository

class KelvinSakService(
    private val sakStatusRepository: SakStatusRepository,
    private val oppgaveGatewayConfig: OppgaveGatewayConfig
) {

    /**
     * TODO: lag person-tabell, slik at vi slipper å spørre på hver ident
     */
    fun hentSakStatus(identer: List<String>): List<SakStatus> {

        return identer.flatMap { ident ->
            val (enhetinfo, saksnummer) = OppgaveGateway(oppgaveGatewayConfig).hentEnhetForPerson(
                ident
            ) ?: Pair(null, null)

            sakStatusRepository.hentSakStatus(ident)
                .map {
                    if (enhetinfo != null && it.sakId == saksnummer) it.copy(
                        enhet = NåværendeEnhet(
                            oversendtDato = enhetinfo.oversendtDato,
                            oppgaveKategori = enhetinfo.oppgaveKategori,
                            enhet = enhetinfo.enhet,
                        )
                    ) else it
                }
        }
    }

    fun hentSakStatus(ident: String): List<SakStatus> {
        return hentSakStatus(listOf(ident))
    }
}