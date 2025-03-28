package api.arena

import api.perioder.PerioderInkludert11_17Response
import api.perioder.PerioderResponse
import no.nav.aap.api.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import java.util.*

interface IArenaoppslagRestClient {
    fun hentPerioder(callId: UUID, vedtakRequest: InternVedtakRequest): PerioderResponse
    fun hentPerioderInkludert11_17(callId: UUID, vedtakRequest: InternVedtakRequest): PerioderInkludert11_17Response
    fun hentPersonEksistererIAapContext(callId: UUID, sakerRequest: SakerRequest): PersonEksistererIAAPArena
    fun hentSakerByFnr(callId: UUID, req: SakerRequest): List<SakStatus>
    fun hentMaksimum(callId: UUID, req: InternVedtakRequest): Maksimum
}