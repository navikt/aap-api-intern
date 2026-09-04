package no.nav.aap.api.syfo

import no.nav.aap.api.arena.ArenaService
import no.nav.aap.api.arena.ArenaVedtakFilter
import no.nav.aap.api.intern.Periode
import no.nav.aap.api.intern.SyfoSak
import no.nav.aap.api.intern.SyfoSakerResponse
import no.nav.aap.api.intern.SyfoVedtak
import no.nav.aap.api.maksimum.InternVedtakUtenUtbetaling
import no.nav.aap.api.sak.SakStatus
import no.nav.aap.api.sak.tilKontrakt
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.komponenter.verdityper.Tid

internal class SyfoService(
    private val arenaService: ArenaService,
) {
    suspend fun hentSaker(
        callId: String,
        personidenter: List<String>,
        kelvinSaker: List<SakStatus.Kelvin>,
    ): SyfoSakerResponse {
        val arenaSaker = arenaService.hentSaker(callId, personidenter)
        val arenaVedtak = hentArenaVedtak(callId, personidenter, arenaSaker)
        val filtrerteArenaVedtak = ArenaVedtakFilter.filtrerUgyldigeVedtak(arenaVedtak)

        return SyfoSakerResponse(
            saker = kelvinSaker.tilSyfoSaker() +
                    arenaSaker.tilSyfoSaker(filtrerteArenaVedtak.tilSyfoVedtak()),
        )
    }

    private suspend fun hentArenaVedtak(
        callId: String,
        personidenter: List<String>,
        saker: List<SakStatus.Arena>,
    ): List<InternVedtakUtenUtbetaling> =
        saker.flatMap { sak ->
            personidenter.flatMap { personident ->
                arenaService.hentVedtakUtenUtbetaling(
                    callId,
                    InternVedtakRequest(
                        personidentifikator = personident,
                        fraOgMedDato = sak.periode.fraOgMedDato ?: Tid.MIN,
                        tilOgMedDato = sak.periode.tilOgMedDato ?: Tid.MAKS,
                    ),
                )
            }
        }
}

private fun List<SakStatus.Kelvin>.tilSyfoSaker(): List<SyfoSak.Kelvin> =
    map { sak ->
        SyfoSak.Kelvin(
            sakid = sak.sakId,
            statuskode = sak.statusKode.tilKontrakt(),
            soknadsdatoer = sak.soknadsdatoer,
            vedtak = sak.vedtaksdato?.let { vedtaksdato ->
                listOf(
                    SyfoVedtak(
                        vedtaksdato = vedtaksdato,
                        perioder = sak.perioder.map { it.tilKontrakt() },
                    ),
                )
            }.orEmpty(),
        )
    }

private fun List<SakStatus.Arena>.tilSyfoSaker(
    vedtak: Map<String, List<SyfoVedtak>>,
): List<SyfoSak.Arena> =
    distinctBy(SakStatus.Arena::sakId).map { sak ->
        SyfoSak.Arena(
            sakid = sak.sakId,
            statuskode = sak.statusKode.tilKontrakt(),
            vedtak = vedtak[sak.sakId].orEmpty(),
        )
    }

private fun List<InternVedtakUtenUtbetaling>.tilSyfoVedtak(): Map<String, List<SyfoVedtak>> =
    groupBy { it.saksnummer }
        .mapValues { (_, vedtak) ->
            vedtak
                .groupBy { it.vedtakId }
                .values
                .map { internVedtak ->
                    SyfoVedtak(
                        vedtaksdato = internVedtak.first().vedtaksdato,
                        perioder = internVedtak.map {
                            Periode(
                                fraOgMedDato = it.periode.fraOgMedDato,
                                tilOgMedDato = it.periode.tilOgMedDato,
                            )
                        }.distinct(),
                    )
                }
                .sortedBy(SyfoVedtak::vedtaksdato)
        }
