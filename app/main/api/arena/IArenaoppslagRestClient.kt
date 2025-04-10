package api.arena

import no.nav.aap.api.intern.PerioderInkludert11_17Response
import no.nav.aap.api.intern.PerioderResponse
import no.nav.aap.api.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import java.util.*

interface IArenaoppslagRestClient {
    fun hentPerioder(callId: String, vedtakRequest: InternVedtakRequest): PerioderResponse
    fun hentPerioderInkludert11_17(callId: String, vedtakRequest: InternVedtakRequest): PerioderMed11_17Response
    fun hentPersonEksistererIAapContext(callId: String, sakerRequest: SakerRequest): PersonEksistererIAAPArena
    fun hentSakerByFnr(callId: String, req: SakerRequest): List<no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus>
    fun hentMaksimum(callId: String, req: InternVedtakRequest): Maksimum
}