package api.arena

import no.nav.aap.api.intern.PerioderInkludert11_17Response
import no.nav.aap.api.intern.PerioderResponse
import no.nav.aap.api.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.personEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import java.util.*

interface IArenaoppslagRestClient {
    fun hentPerioder(callId: UUID, vedtakRequest: InternVedtakRequest): PerioderResponse
    fun hentPerioderInkludert11_17(callId: UUID, vedtakRequest: InternVedtakRequest): PerioderInkludert11_17Response
    fun hentPersonEksistererIAapContext(callId: UUID, sakerRequest: SakerRequest): personEksistererIAAPArena
    fun hentSakerByFnr(callId: UUID, req: SakerRequest): List<SakStatus>
    fun hentMaksimum(callId: UUID, req: InternVedtakRequest): Maksimum
}