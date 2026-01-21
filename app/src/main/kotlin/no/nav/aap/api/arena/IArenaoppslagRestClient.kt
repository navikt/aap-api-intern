package no.nav.aap.api.arena

import no.nav.aap.api.intern.PerioderResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.arenaoppslag.kontrakt.intern.PersonEksistererIAAPArena
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum

interface IArenaoppslagRestClient {
    suspend fun hentPerioder(callId: String, vedtakRequest: InternVedtakRequest): PerioderResponse
    suspend fun hentPerioderInkludert11_17(
        callId: String,
        req: InternVedtakRequest
    ): PerioderMed11_17Response

    suspend fun hentPersonEksistererIAapContext(
        callId: String,
        req: SakerRequest
    ): PersonEksistererIAAPArena

    suspend fun hentPersonHarSignifikantHistorikk(
        callId: String,
        req: SignifikanteSakerRequest
    ): SignifikanteSakerResponse

    suspend fun hentSakerByFnr(
        callId: String,
        req: SakerRequest
    ): List<no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus>

    suspend fun hentMaksimum(callId: String, req: InternVedtakRequest): Maksimum
}