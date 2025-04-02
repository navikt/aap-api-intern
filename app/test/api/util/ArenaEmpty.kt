package api.util

import api.arena.IArenaoppslagRestClient
import no.nav.aap.api.intern.PerioderResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import java.util.*

class ArenaClient : IArenaoppslagRestClient {
    override fun hentPerioder(callId: UUID, vedtakRequest: InternVedtakRequest): PerioderResponse {
        return PerioderResponse(emptyList())
    }

    override fun hentPerioderInkludert11_17(
        callId: UUID,
        vedtakRequest: InternVedtakRequest
    ): PerioderMed11_17Response {
        return PerioderMed11_17Response(emptyList())
    }

    override fun hentPersonEksistererIAapContext(
        callId: UUID,
        sakerRequest: SakerRequest
    ): PersonEksistererIAAPArena {
        return PersonEksistererIAAPArena(false)
    }

    override fun hentSakerByFnr(
        callId: UUID,
        req: SakerRequest
    ): List<no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus> {
        return emptyList()
    }

    override fun hentMaksimum(callId: UUID, req: InternVedtakRequest): Maksimum {
        return Maksimum(emptyList())
    }
}