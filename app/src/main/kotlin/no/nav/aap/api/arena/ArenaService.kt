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
    private val arena: IArenaoppslagGateway,
    private val arenaHistorikk: IArenaoppslagGateway
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

    suspend fun hentSaker(callId: String, personIdenter: List<String>): List<SakStatus> {
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