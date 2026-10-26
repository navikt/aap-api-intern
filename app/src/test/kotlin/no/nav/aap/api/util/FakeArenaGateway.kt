package no.nav.aap.api.util

import no.nav.aap.api.arena.IArenaoppslagGateway
import no.nav.aap.api.intern.PerioderResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakOppsummeringKontrakt
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkRequest
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.ManuellFordelingsgrunnlagResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.PerioderMed11_17Response
import no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum
import java.util.concurrent.ConcurrentHashMap
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakMedVedtakResponse as ArenaSakMedVedtakResponseV1
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerRequest as SakerRequestV1

// Ukjente identer gir tomme svar, slik den ekte gatewayen gjør når Arena svarer 404.
class FakeArenaGateway : IArenaoppslagGateway {
    private val saker = ConcurrentHashMap<String, List<SakStatus>>()
    private val sakerForPerson = ConcurrentHashMap<String, List<ArenaSakOppsummeringKontrakt>>()
    private val maksimum = ConcurrentHashMap<String, Maksimum>()
    private val perioder = ConcurrentHashMap<String, PerioderResponse>()
    private val perioderInkludert11_17 = ConcurrentHashMap<String, PerioderMed11_17Response>()
    private val identerMedHistorikk = ConcurrentHashMap.newKeySet<String>()
    private val sakerMedVedtak = ConcurrentHashMap<String, ArenaSakMedVedtakResponseV1>()
    private val manuellFordelingsgrunnlag =
        ConcurrentHashMap<String, ManuellFordelingsgrunnlagResponse>()

    fun leggTilSaker(ident: String, saker: List<SakStatus>) = apply { this.saker[ident] = saker }

    fun leggTilSakerForPerson(ident: String, saker: List<ArenaSakOppsummeringKontrakt>) =
        apply { sakerForPerson[ident] = saker }

    fun leggTilMaksimum(ident: String, maksimum: Maksimum) =
        apply { this.maksimum[ident] = maksimum }

    fun leggTilPerioder(ident: String, perioder: PerioderResponse) =
        apply { this.perioder[ident] = perioder }

    fun leggTilPerioderInkludert11_17(ident: String, perioder: PerioderMed11_17Response) =
        apply { perioderInkludert11_17[ident] = perioder }

    fun leggTilHistorikk(ident: String) = apply { identerMedHistorikk.add(ident) }

    fun leggTilSakMedVedtak(sak: ArenaSakMedVedtakResponseV1) =
        apply { sakerMedVedtak[sak.sakId] = sak }

    fun leggTilManuellFordelingsgrunnlag(
        ident: String,
        grunnlag: ManuellFordelingsgrunnlagResponse
    ) =
        apply { manuellFordelingsgrunnlag[ident] = grunnlag }

    override suspend fun hentPerioder(
        callId: String,
        vedtakRequest: InternVedtakRequest
    ): PerioderResponse =
        perioder[vedtakRequest.personidentifikator] ?: PerioderResponse(emptyList())

    override suspend fun hentPerioderInkludert11_17(
        callId: String, req: InternVedtakRequest
    ): PerioderMed11_17Response =
        perioderInkludert11_17[req.personidentifikator] ?: PerioderMed11_17Response(emptyList())

    override suspend fun hentPersonHarHistorikkIArena(
        callId: String,
        req: HarHistorikkRequest
    ): HarHistorikkResponse = HarHistorikkResponse(req.personidentifikator in identerMedHistorikk)

    override suspend fun hentSakerByFnr(callId: String, req: SakerRequest): List<SakStatus> =
        req.personidentifikatorer.flatMap { saker[it].orEmpty() }

    override suspend fun hentSakerForPerson(callId: String, req: SakerRequestV1): SakerResponse =
        SakerResponse(sakerForPerson[req.personidentifikator].orEmpty())

    override suspend fun hentMaksimum(callId: String, req: InternVedtakRequest): Maksimum =
        maksimum[req.personidentifikator] ?: Maksimum(emptyList())

    override suspend fun hentArenaSakMedVedtak(
        callId: String,
        sakId: String
    ): ArenaSakMedVedtakResponseV1? =
        sakerMedVedtak[sakId]

    override suspend fun hentManuellFordelingsgrunnlag(
        callId: String, personidentifikator: String
    ): ManuellFordelingsgrunnlagResponse? = manuellFordelingsgrunnlag[personidentifikator]
}
