package no.nav.aap.api.aren

import no.nav.aap.api.arena.IArenaoppslagRestClient
import no.nav.aap.api.intern.ArenaStatusResponse
import no.nav.aap.api.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest as ArenaSakerRequest

class ArenaService(private val arena: IArenaoppslagRestClient) {

    suspend fun eksistererIAapArena(personidentifikatorer: List<String>, callId: String): PersonEksistererIAAPArena {
        val aapHistorikkForPerson = arena.hentPersonEksistererIAapContext(callId, ArenaSakerRequest(personidentifikatorer))
        return PersonEksistererIAAPArena(aapHistorikkForPerson.eksisterer)
    }

    suspend fun kanBehandlesIKelvin(personidentifikatorer: List<String>, callId: String): ArenaStatusResponse {
        val kanBehandlesIKelvin =  arena.personKanBehandlesIKelvin(callId, ArenaSakerRequest(personidentifikatorer))
        return ArenaStatusResponse(kanBehandlesIKelvin.kanBehandles, kanBehandlesIKelvin.nyesteArenaSakId)
    }

}
