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
    override fun hentPerioder(callId: String, vedtakRequest: InternVedtakRequest): PerioderResponse {
        return PerioderResponse(emptyList())
    }

    override fun hentPerioderInkludert11_17(
        callId: String,
        vedtakRequest: InternVedtakRequest
    ): PerioderMed11_17Response {
        return PerioderMed11_17Response(emptyList())
    }

    override fun hentPersonEksistererIAapContext(
        callId: String,
        sakerRequest: SakerRequest
    ): PersonEksistererIAAPArena {
        return PersonEksistererIAAPArena(false)
    }

    override fun hentSakerByFnr(
        callId: String,
        req: SakerRequest
    ): List<no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus> {
        return emptyList()
    }

    override fun hentMaksimum(callId: String, req: InternVedtakRequest): Maksimum {
        return Maksimum(emptyList())
    }
}