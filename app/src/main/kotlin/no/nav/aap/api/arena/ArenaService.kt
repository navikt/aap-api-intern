package no.nav.aap.api.arena

import no.nav.aap.api.intern.ArenaSakMedVedtakResponse
import no.nav.aap.api.intern.ArenaSakOppsummering
import no.nav.aap.api.intern.ArenaSakerResponse
import no.nav.aap.api.intern.ArenaSakPerson
import no.nav.aap.api.intern.ArenaVedtakDetaljer
import no.nav.aap.api.intern.ArenaVedtakfakta
import no.nav.aap.api.intern.Periode
import no.nav.aap.api.intern.PeriodeInkludert11_17
import no.nav.aap.api.intern.PerioderInkludert11_17Response
import no.nav.aap.api.intern.PersonEksistererIAAPArena
import no.nav.aap.api.intern.SakStatus
import no.nav.aap.api.maksimum.InternVedtakUtenUtbetaling
import no.nav.aap.api.maksimum.InternVedtak
import no.nav.aap.api.util.fraKontrakt
import no.nav.aap.api.util.fraKontraktUtenUtbetaling
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakMedVedtakResponse as ArenaSakMedVedtakResponseV1
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerResponse
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.Status
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerRequest as SakerRequestV1

class ArenaService(
    private val arena: IArenaoppslagGateway,
    private val arenaHistorikk: IArenaoppslagGateway
) {


    suspend fun eksistererIAapArena(callId: String, personIdenter: List<String>): PersonEksistererIAAPArena {
        val aapHistorikkForPerson = arenaHistorikk.hentPersonEksistererIAapContext(callId, SakerRequest(personIdenter))
        return PersonEksistererIAAPArena(aapHistorikkForPerson.eksisterer)
    }

    suspend fun aktivitetfase(callId: String, vedtakRequest: InternVedtakRequest): PerioderInkludert11_17Response {
        val arenaSvar = arena.hentPerioderInkludert11_17(callId, vedtakRequest)

        return PerioderInkludert11_17Response(
            perioder = arenaSvar.perioder.map { periode ->
                PeriodeInkludert11_17(
                    periode = periode.periode.let {
                        Periode(
                            it.fraOgMedDato,
                            it.tilOgMedDato
                        )
                    },
                    aktivitetsfaseKode = periode.aktivitetsfaseKode,
                    aktivitetsfaseNavn = periode.aktivitetsfaseNavn,
                )
            }
        )
    }

    suspend fun hentSaker(callId: String, personIdenter: List<String>): List<SakStatus.Arena> {
        val sakerRequest = SakerRequest(personIdenter)
        return arena.hentSakerByFnr(callId, sakerRequest).map {
            arenaSakStatusTilDomene(it)
        }
    }

    private fun arenaSakStatusTilDomene(it: no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus) =
        SakStatus.Arena(
            sakId = it.sakId,
            statusKode = when (it.statusKode) {
                Status.AVSLU -> no.nav.aap.api.intern.ArenaStatus.AVSLU
                Status.FORDE -> no.nav.aap.api.intern.ArenaStatus.FORDE
                Status.GODKJ -> no.nav.aap.api.intern.ArenaStatus.GODKJ
                Status.INNST -> no.nav.aap.api.intern.ArenaStatus.INNST
                Status.IVERK -> no.nav.aap.api.intern.ArenaStatus.IVERK
                Status.KONT -> no.nav.aap.api.intern.ArenaStatus.KONT
                Status.MOTAT -> no.nav.aap.api.intern.ArenaStatus.MOTAT
                Status.OPPRE -> no.nav.aap.api.intern.ArenaStatus.OPPRE
                Status.REGIS -> no.nav.aap.api.intern.ArenaStatus.REGIS
                Status.UKJENT -> no.nav.aap.api.intern.ArenaStatus.UKJENT
            },
            periode = Periode(
                it.periode.fraOgMedDato,
                it.periode.tilOgMedDato
            )
        )

    suspend fun hentPerioder(callId: String, vedtakRequest: InternVedtakRequest): List<Periode> {
        return arena.hentPerioder(callId, vedtakRequest).perioder
    }

    suspend fun hentSakerForPerson(callId: String, personidentifikator: String): ArenaSakerResponse {
        return arena.hentSakerForPerson(callId, SakerRequestV1(personidentifikator)).toResponse()
    }

    suspend fun hentVedtakUtenUtbetaling(
        callId: String,
        vedtakRequest: InternVedtakRequest
    ): List<InternVedtakUtenUtbetaling> {
        return arena.hentMaksimum(callId, vedtakRequest).vedtak.map { it.fraKontraktUtenUtbetaling() }
    }

    suspend fun hentVedtak(callId: String, vedtakRequest: InternVedtakRequest): List<InternVedtak> {
        return arena.hentMaksimum(callId, vedtakRequest).fraKontrakt().vedtak
    }

    suspend fun hentArenaSakMedVedtak(callId: String, sakId: String): ArenaSakMedVedtakResponse? =
        arena.hentArenaSakMedVedtak(callId, sakId)?.toInternResponse()

}


private fun SakerResponse.toResponse() = ArenaSakerResponse(
    saker = saker.map { sak ->
        ArenaSakOppsummering(
            sakId = sak.sakId,
            lopenummer = sak.lopenummer,
            aar = sak.aar,
            antallVedtak = sak.antallVedtak,
            statuskode = sak.statuskode,
            statusnavn = sak.statusnavn,
            sakstype = sak.sakstype,
            regDato = sak.regDato,
            avsluttetDato = sak.avsluttetDato,
        )
    }
)

private fun ArenaSakMedVedtakResponseV1.toInternResponse() = ArenaSakMedVedtakResponse(
    sakId = sakId,
    opprettetAar = opprettetAar,
    lopenr = lopenr,
    person = ArenaSakPerson(
        personId = person.personId,
        fodselsnummer = person.fodselsnummer,
        fornavn = person.fornavn,
        etternavn = person.etternavn,
    ),
    statuskode = statuskode,
    statusnavn = statusnavn,
    registrertDato = registrertDato,
    avsluttetDato = avsluttetDato,
    vedtak = vedtak.map { v ->
        ArenaVedtakDetaljer(
            vedtakId = v.vedtakId,
            lopenrvedtak = v.lopenrvedtak,
            statusKode = v.statusKode,
            statusNavn = v.statusNavn,
            vedtaktypeKode = v.vedtaktypeKode,
            vedtaktypeNavn = v.vedtaktypeNavn,
            aktivitetsfaseKode = v.aktivitetsfaseKode,
            aktivitetsfaseNavn = v.aktivitetsfaseNavn,
            fraOgMed = v.fraOgMed,
            tilDato = v.tilDato,
            rettighetkode = v.rettighetkode,
            rettighetnavn = v.rettighetnavn,
            utfallkode = v.utfallkode,
            begrunnelse = v.begrunnelse,
            saksbehandler = v.saksbehandler,
            beslutter = v.beslutter,
            relatertVedtak = v.relatertVedtak,
            fakta = v.fakta.map { f ->
                ArenaVedtakfakta(
                    kode = f.kode,
                    navn = f.navn,
                    verdi = f.verdi,
                    registrertDato = f.registrertDato,
                )
            },
        )
    },
)