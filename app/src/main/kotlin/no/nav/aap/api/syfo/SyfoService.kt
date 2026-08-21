package no.nav.aap.api.syfo

import no.nav.aap.api.arena.ArenaService
import no.nav.aap.api.hentAllePersonidenter
import no.nav.aap.api.intern.Kilde
import no.nav.aap.api.intern.Periode
import no.nav.aap.api.intern.SakStatus
import no.nav.aap.api.intern.SyfoSakerResponse
import no.nav.aap.api.intern.SyfoSoknader
import no.nav.aap.api.intern.SyfoVedtak
import no.nav.aap.api.kelvin.Behandling
import no.nav.aap.api.kelvin.KelvinSakService
import no.nav.aap.api.maksimum.InternVedtakUtenUtbetaling
import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.api.postgres.SakStatusRepository
import no.nav.aap.arenaoppslag.kontrakt.intern.InternVedtakRequest
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.verdityper.Tid
import javax.sql.DataSource
import no.nav.aap.komponenter.type.Periode as KelvinPeriode

class SyfoService(
    private val dataSource: DataSource,
    private val arenaService: ArenaService,
    private val pdlGateway: IPdlGateway,
) {
    suspend fun hentSaker(callId: String, personidentifikator: String): SyfoSakerResponse {
        val personidenter = hentAllePersonidenter(listOf(personidentifikator), pdlGateway)
        val (kelvinSaker, kelvinVedtak) = hentKelvinData(personidenter)
        val arenaSaker = arenaService.hentSaker(callId, personidenter)
        val arenaVedtak = hentArenaVedtak(callId, personidenter, arenaSaker)

        return SyfoSakerResponse(
            soknader = kelvinSaker
                .distinctBy { it.sakId }
                .map {
                    SyfoSoknader(
                        sakId = it.sakId,
                        statuskode = it.statusKode,
                        soknadsdatoer = it.soknadsdatoer.distinct().sorted(),
                    )
                }
                .sortedBy(SyfoSoknader::sakId),
            vedtak = (arenaVedtak.tilSyfoVedtak() + kelvinVedtak.map(KelvinVedtak::tilSyfoVedtak))
                .sortedBy(SyfoVedtak::vedtaksdato),
        )
    }

    private fun hentKelvinData(
        personidenter: List<String>,
    ): Pair<List<SakStatus.Kelvin>, List<KelvinVedtak>> =
        dataSource.transaction { connection ->
            val behandlingsRepository = BehandlingsRepository(connection)
            val saker = KelvinSakService(
                SakStatusRepository(connection),
                behandlingsRepository,
            ).hentSakStatus(personidenter)
            val vedtak = personidenter
                .flatMap { personident ->
                    behandlingsRepository.hentVedtaksData(
                        personident,
                        KelvinPeriode(Tid.MIN, Tid.MAKS),
                    )
                }
                .distinctBy {
                    Triple(
                        it.sak.saksnummer,
                        it.vedtakId,
                        it.vedtaksDato,
                    )
                }
                .map(::tilKelvinVedtak)

            saker to vedtak
        }

    private fun tilKelvinVedtak(behandling: Behandling): KelvinVedtak =
        KelvinVedtak(
            sakId = behandling.sak.saksnummer,
            vedtaksdato = behandling.vedtaksDato,
            perioder = behandling.rettighetsTypePerioder
                .map { Periode(it.fom, it.tom) }
                .distinct(),
        )

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

    private fun List<InternVedtakUtenUtbetaling>.tilSyfoVedtak(): List<SyfoVedtak> =
        groupBy { it.saksnummer to it.vedtakId }
            .values
            .map { vedtak ->
                SyfoVedtak(
                    sakId = vedtak.first().saksnummer,
                    kilde = Kilde.ARENA,
                    vedtaksdato = vedtak.first().vedtaksdato,
                    perioder = vedtak.map {
                        Periode(
                            fraOgMedDato = it.periode.fraOgMedDato,
                            tilOgMedDato = it.periode.tilOgMedDato,
                        )
                    }.distinct(),
                )
            }
}

private data class KelvinVedtak(
    val sakId: String,
    val vedtaksdato: java.time.LocalDate,
    val perioder: List<Periode>,
) {
    fun tilSyfoVedtak(): SyfoVedtak =
        SyfoVedtak(
            sakId = sakId,
            kilde = Kilde.KELVIN,
            vedtaksdato = vedtaksdato,
            perioder = perioder,
        )
}
