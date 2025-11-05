package no.nav.aap.api.util

import no.nav.aap.api.arena.IArenaoppslagRestClient
import no.nav.aap.api.intern.PerioderResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.*
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum

class MockedArenaClient : IArenaoppslagRestClient {
    override suspend fun hentPerioder(callId: String, vedtakRequest: InternVedtakRequest): PerioderResponse {
        return PerioderResponse(emptyList())
    }

    override suspend fun hentPerioderInkludert11_17(
        callId: String, req: InternVedtakRequest
    ): PerioderMed11_17Response {
        return PerioderMed11_17Response(emptyList())
    }

    override suspend fun hentPersonEksistererIAapContext(
        callId: String, req: SakerRequest
    ): PersonEksistererIAAPArena {
        return PersonEksistererIAAPArena(false)
    }

    override suspend fun hentSakerByFnr(
        callId: String, req: SakerRequest
    ): List<SakStatus> {
        return emptyList()
    }

    override suspend fun hentMaksimum(
        callId: String, req: InternVedtakRequest
    ): Maksimum {
        return Maksimum(emptyList())
    }
}