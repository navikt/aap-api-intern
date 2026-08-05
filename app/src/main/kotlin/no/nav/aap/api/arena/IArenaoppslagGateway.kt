package no.nav.aap.api.arena

import no.nav.aap.api.intern.PerioderResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakMedVedtakResponse as ArenaSakMedVedtakResponseV1
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.ManuellFordelingsgrunnlagResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerRequest as SakerRequestV1

interface IArenaoppslagGateway {
    suspend fun hentPerioder(callId: String, vedtakRequest: InternVedtakRequest): PerioderResponse
    @Suppress("FunctionName")
    suspend fun hentPerioderInkludert11_17(
        callId: String,
        req: InternVedtakRequest
    ): PerioderMed11_17Response

    suspend fun hentPersonHarHistorikkIArena(
        callId: String,
        req: HarHistorikkRequest
    ): HarHistorikkResponse


    suspend fun hentSakerByFnr(
        callId: String,
        req: SakerRequest
    ): List<no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus>

    suspend fun hentSakerForPerson(
        callId: String,
        req: SakerRequestV1
    ): SakerResponse

    suspend fun hentArenaSakMedVedtak(
        callId: String,
        sakId: String
    ): ArenaSakMedVedtakResponseV1?

    suspend fun hentMaksimum(callId: String, req: InternVedtakRequest): Maksimum

    suspend fun hentManuellFordelingsgrunnlag(
        callId: String,
        personidentifikator: String
    ): ManuellFordelingsgrunnlagResponse?
}