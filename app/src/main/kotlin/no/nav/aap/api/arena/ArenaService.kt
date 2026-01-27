package no.nav.aap.api.arena

import no.nav.aap.api.intern.Periode
import no.nav.aap.api.intern.PeriodeInkludert11_17
import no.nav.aap.api.intern.PerioderInkludert11_17Response
import no.nav.aap.api.intern.PersonEksistererIAAPArena
import no.nav.aap.api.intern.SakStatus
import no.nav.aap.api.intern.SignifikanteSakerResponse
import no.nav.aap.api.intern.Vedtak
import no.nav.aap.api.intern.VedtakUtenUtbetaling
import no.nav.aap.api.util.fraKontrakt
import no.nav.aap.api.util.fraKontraktUtenUtbetaling
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.Kilde
import no.nav.aap.arenaoppslag.kontrakt.intern.SakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.SignifikanteSakerRequest
import no.nav.aap.arenaoppslag.kontrakt.intern.Status
import java.time.LocalDate

class ArenaService(
    private val arena: IArenaoppslagRestClient,
    private val arenaHistorikk: IArenaoppslagRestClient
) {

    suspend fun eksistererIAapArena(callId: String, personIdenter: List<String>): PersonEksistererIAAPArena {
        val aapHistorikkForPerson = arenaHistorikk.hentPersonEksistererIAapContext(callId, SakerRequest(personIdenter))
        return PersonEksistererIAAPArena(aapHistorikkForPerson.eksisterer)
    }

    suspend fun harSignifikantAAPArenaHistorikk(
        callId: String,
        personIdenter: List<String>,
        virkningstidspunkt: LocalDate
    ): SignifikanteSakerResponse {
        val harSignifikantAAPArenaHistorikk =
            arenaHistorikk.hentPersonHarSignifikantHistorikk(
                callId,
                SignifikanteSakerRequest(personIdenter, virkningstidspunkt)
            )
        return SignifikanteSakerResponse(
            harSignifikantAAPArenaHistorikk.harSignifikantHistorikk,
            harSignifikantAAPArenaHistorikk.signifikanteSaker
        )
    }

    suspend fun aktivitetfase(callId: String, vedtakRequest: InternVedtakRequest): PerioderInkludert11_17Response {
        val arenaSvar = arena.hentPerioderInkludert11_17(callId, vedtakRequest)

        return PerioderInkludert11_17Response(
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
    }

    suspend fun hentSaker(callId: String, personIdenter: List<String>): List<SakStatus> {
        val sakerRequest = SakerRequest(personIdenter)
        return arena.hentSakerByFnr(callId, sakerRequest).map {
            arenaSakStatusTilDomene(it)
        }
    }

    private fun arenaSakStatusTilDomene(it: no.nav.aap.arenaoppslag.kontrakt.intern.SakStatus) =
        SakStatus(
            sakId = it.sakId,
            statusKode = when (it.statusKode) {
                Status.AVSLU -> no.nav.aap.api.intern.Status.AVSLU
                Status.FORDE -> no.nav.aap.api.intern.Status.FORDE
                Status.GODKJ -> no.nav.aap.api.intern.Status.GODKJ
                Status.INNST -> no.nav.aap.api.intern.Status.INNST
                Status.IVERK -> no.nav.aap.api.intern.Status.IVERK
                Status.KONT -> no.nav.aap.api.intern.Status.KONT
                Status.MOTAT -> no.nav.aap.api.intern.Status.MOTAT
                Status.OPPRE -> no.nav.aap.api.intern.Status.OPPRE
                Status.REGIS -> no.nav.aap.api.intern.Status.REGIS
                Status.UKJENT -> no.nav.aap.api.intern.Status.UKJENT
                Status.OPPRETTET -> no.nav.aap.api.intern.Status.OPPRETTET
                Status.UTREDES -> no.nav.aap.api.intern.Status.UTREDES
                Status.LØPENDE -> no.nav.aap.api.intern.Status.LØPENDE
                Status.AVSLUTTET -> no.nav.aap.api.intern.Status.AVSLUTTET
            },
            periode = Periode(
                it.periode.fraOgMedDato,
                it.periode.tilOgMedDato
            ),
            kilde = when (it.kilde) {
                Kilde.ARENA -> no.nav.aap.api.intern.Kilde.ARENA
                Kilde.KELVIN -> no.nav.aap.api.intern.Kilde.KELVIN
            }
        )

    suspend fun hentPerioder(callId: String, vedtakRequest: InternVedtakRequest): List<Periode> {
        return arena.hentPerioder(callId, vedtakRequest).perioder
    }

    suspend fun hentVedtakUtenUtbetaling(
        callId: String,
        vedtakRequest: InternVedtakRequest
    ): List<VedtakUtenUtbetaling> {
        return arena.hentMaksimum(callId, vedtakRequest).vedtak.map { it.fraKontraktUtenUtbetaling() }
    }

    suspend fun hentVedtak(callId: String, vedtakRequest: InternVedtakRequest): List<Vedtak> {
        return arena.hentMaksimum(callId, vedtakRequest).fraKontrakt().vedtak
    }

}