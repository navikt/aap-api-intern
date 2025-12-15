package no.nav.aap.api.aren

import no.nav.aap.api.arena.IArenaoppslagRestClient
import no.nav.aap.api.intern.ArenaStatusResponse
import no.nav.aap.api.intern.Kilde
import no.nav.aap.api.intern.Periode
import no.nav.aap.api.intern.PeriodeInkludert11_17
import no.nav.aap.api.intern.PerioderInkludert11_17Response
import no.nav.aap.api.intern.PersonEksistererIAAPArena
import no.nav.aap.api.intern.SakStatus
import no.nav.aap.api.intern.VedtakUtenUtbetaling
import no.nav.aap.api.util.fraKontrakt
import no.nav.aap.api.util.fraKontraktUtenUtbetaling
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import kotlin.collections.map
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest as ArenaSakerRequest

class ArenaService(private val arena: IArenaoppslagRestClient) {

    suspend fun eksistererIAapArena(personidentifikatorer: List<String>, callId: String): PersonEksistererIAAPArena {
        val aapHistorikkForPerson = arena.hentPersonEksistererIAapContext(callId, ArenaSakerRequest(personidentifikatorer))
        return PersonEksistererIAAPArena(aapHistorikkForPerson.eksisterer)
    }

    suspend fun kanBehandlesIKelvin(personidentifikatorer: List<String>, callId: String): ArenaStatusResponse {
        val kanBehandlesIKelvin =  arena.personKanBehandlesIKelvin(callId, ArenaSakerRequest(personidentifikatorer))
        return ArenaStatusResponse(kanBehandlesIKelvin.kanBehandles, kanBehandlesIKelvin.nyesteArenaSakId)
    }

    suspend fun aktivitetfase(callId: String, vedtakRequest: InternVedtakRequest): PerioderInkludert11_17Response {
        val arenaSvar = arena.hentPerioderInkludert11_17(callId, vedtakRequest)

        val aktivitetIPerioder = PerioderInkludert11_17Response(
            perioder = arenaSvar.perioder.map {
                PeriodeInkludert11_17(
                    periode = it.periode.let {
                        Periode(
                            it.fraOgMedDato,
                            it.tilOgMedDato
                        )
                    },
                    aktivitetsfaseKode = it.aktivitetsfaseKode,
                    aktivitetsfaseNavn = it.aktivitetsfaseNavn,
                )
            }
        )
        return aktivitetIPerioder
    }

    suspend fun hentSaker(callId: String, personIdenter: List<String>): List<SakStatus> {
        val sakerRequest = ArenaSakerRequest(personIdenter)
        return arena.hentSakerByFnr(callId, sakerRequest).map {
            arenaSakStatusTilDomene(it)
        }
    }

    private fun arenaSakStatusTilDomene(it: no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus) =
        SakStatus(
            sakId = it.sakId,
            statusKode = when (it.statusKode) {
                no.nav.aap.arenaoppslag.kontrakt.intern.Status.AVSLU -> no.nav.aap.api.intern.Status.AVSLU
                no.nav.aap.arenaoppslag.kontrakt.intern.Status.FORDE -> no.nav.aap.api.intern.Status.FORDE
                no.nav.aap.arenaoppslag.kontrakt.intern.Status.GODKJ -> no.nav.aap.api.intern.Status.GODKJ
                no.nav.aap.arenaoppslag.kontrakt.intern.Status.INNST -> no.nav.aap.api.intern.Status.INNST
                no.nav.aap.arenaoppslag.kontrakt.intern.Status.IVERK -> no.nav.aap.api.intern.Status.IVERK
                no.nav.aap.arenaoppslag.kontrakt.intern.Status.KONT -> no.nav.aap.api.intern.Status.KONT
                no.nav.aap.arenaoppslag.kontrakt.intern.Status.MOTAT -> no.nav.aap.api.intern.Status.MOTAT
                no.nav.aap.arenaoppslag.kontrakt.intern.Status.OPPRE -> no.nav.aap.api.intern.Status.OPPRE
                no.nav.aap.arenaoppslag.kontrakt.intern.Status.REGIS -> no.nav.aap.api.intern.Status.REGIS
                no.nav.aap.arenaoppslag.kontrakt.intern.Status.UKJENT -> no.nav.aap.api.intern.Status.UKJENT
                no.nav.aap.arenaoppslag.kontrakt.intern.Status.OPPRETTET -> no.nav.aap.api.intern.Status.OPPRETTET
                no.nav.aap.arenaoppslag.kontrakt.intern.Status.UTREDES -> no.nav.aap.api.intern.Status.UTREDES
                no.nav.aap.arenaoppslag.kontrakt.intern.Status.LØPENDE -> no.nav.aap.api.intern.Status.LØPENDE
                no.nav.aap.arenaoppslag.kontrakt.intern.Status.AVSLUTTET -> no.nav.aap.api.intern.Status.AVSLUTTET
            },
            periode = Periode(
                it.periode.fraOgMedDato,
                it.periode.tilOgMedDato
            ),
            kilde = when (it.kilde) {
                no.nav.aap.arenaoppslag.kontrakt.intern.Kilde.ARENA -> Kilde.ARENA
                no.nav.aap.arenaoppslag.kontrakt.intern.Kilde.KELVIN -> Kilde.KELVIN
            }
        )

    suspend fun hentPerioder(callId: String, vedtakRequest: InternVedtakRequest): List<Periode> {
        return arena.hentPerioder(callId, vedtakRequest).perioder
    }

    suspend fun hentVedtakUtenUtbetaling(callId: String, vedtakRequest: InternVedtakRequest): List<VedtakUtenUtbetaling> {
        return arena.hentMaksimum(callId, vedtakRequest).vedtak.map { it.fraKontraktUtenUtbetaling() }
    }

    suspend fun hentVedtak(callId: String, vedtakRequest: InternVedtakRequest): List<no.nav.aap.api.intern.Vedtak> {
        return arena.hentMaksimum(callId, vedtakRequest).fraKontrakt().vedtak
    }

}
