package no.nav.aap.api.kelvin

import no.nav.aap.api.intern.*
import no.nav.aap.api.intern.behandlingsflyt.SakstatusFraKelvin
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.api.postgres.SakStatusRepository
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

class KelvinSakService(
    private val sakStatusRepository: SakStatusRepository,
    private val behandlingsRepository: BehandlingsRepository
) {

    /**
     * TODO: lag person-tabell, slik at vi slipper å spørre på hver ident
     */
    fun hentSakStatus(identer: List<String>): List<SakStatus.Kelvin> {

        return identer.flatMap { ident ->
            val (enhetinfo, saksnummer) = OppgaveGateway.hentEnhetForPerson(ident)

            val nyesteBehandling = behandlingsRepository.hentVedtaksData(
                ident,
                no.nav.aap.komponenter.type.Periode(Tid.MIN, Tid.MAKS)
            ).maxByOrNull { it.vedtaksDato }

            val periode = nyesteBehandling?.rettighetsTypeTidslinje.orEmpty().let {
                if (it.isEmpty()) null else it.helePerioden()
            }

            sakStatusRepository.hentSakStatus(ident)
                .map { kelvinSakStatus ->
                    SakStatus.Kelvin(
                        sakId = kelvinSakStatus.sakId,
                        statusKode = when (kelvinSakStatus.statusKode) {
                            SakstatusFraKelvin.OPPRETTET -> KelvinStatus.OPPRETTET
                            SakstatusFraKelvin.UTREDES -> KelvinStatus.OPPRETTET
                            SakstatusFraKelvin.LØPENDE -> KelvinStatus.LØPENDE
                            SakstatusFraKelvin.AVSLUTTET -> KelvinStatus.AVSLUTTET
                            SakstatusFraKelvin.SOKNAD_UNDER_BEHANDLING -> KelvinStatus.SOKNAD_UNDER_BEHANDLING
                            SakstatusFraKelvin.REVURDERING_UNDER_BEHANDLING -> KelvinStatus.REVURDERING_UNDER_BEHANDLING
                            SakstatusFraKelvin.FERDIGBEHANDLET -> KelvinStatus.FERDIGBEHANDLET
                        },
                        ytelsestatus = when {
                            kelvinSakStatus.statusKode == SakstatusFraKelvin.SOKNAD_UNDER_BEHANDLING -> SakStatus.YtelseStatus.FOR_VEDTAK
                            // Behandling er kun null før vedtaket er fattet
                            nyesteBehandling == null -> SakStatus.YtelseStatus.FOR_VEDTAK
                            erAvsluttetNå(
                                nyesteBehandling,
                                LocalDate.now()
                            ) -> SakStatus.YtelseStatus.AVSLUTTET

                            else -> SakStatus.YtelseStatus.LOPENDE
                        },
                        periode = periode?.let { Periode(it.fom, it.tom) }
                            ?: kelvinSakStatus.periode.let { Periode(it.fom, it.tom) },
                        enhet = if (enhetinfo != null && kelvinSakStatus.sakId == saksnummer) NåværendeEnhet(
                            oversendtDato = enhetinfo.oversendtDato,
                            oppgaveKategori = enhetinfo.oppgaveKategori,
                            enhet = enhetinfo.enhet,
                        ) else null,
                        perioder = nyesteBehandling?.rettighetsTypeTidslinje.orEmpty().segmenter()
                            .map { it.periode }.map { Periode(it.fom, it.tom) },
                    )
                }
        }
    }

    fun erAvsluttetNå(behandling: Behandling, nå: LocalDate): Boolean {
        val perioderMedRett = behandling.rettighetsTypeTidslinje.segmenter().map { it.periode }

        val sisteStans = behandling.stansOpphørVurdering.orEmpty().filter { stansEllerOpphør ->
            perioderMedRett.all { periode ->
                periode.tom.isBefore(stansEllerOpphør.fom)
            }
        }.maxByOrNull { it.fom }

        return sisteStans != null && sisteStans.fom.plusDays(1).isBefore(nå)
    }

    fun hentSakStatusUtenEnhet(identer: List<String>): List<SakStatusMeldekortbackend> {
        return identer.flatMap { ident ->
            sakStatusRepository.hentSakStatus(ident)
                .map {
                    SakStatusMeldekortbackend(
                        Kilde.KELVIN,
                        it.periode.let { Periode(it.fom, it.tom) },
                        it.sakId
                    )
                }
        }
    }
}