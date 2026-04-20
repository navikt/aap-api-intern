package no.nav.aap.api.kelvin

import no.nav.aap.api.intern.PeriodeInkludert11_17
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode

class AktivitetsfaseService(
    private val behandlingsRepository: BehandlingsRepository,
) {
    constructor(connection: DBConnection): this(
        behandlingsRepository = BehandlingsRepository(connection),
    )

    fun hentPerioderMedAktivitetsfase(fnr: String, periode: Periode): List<PeriodeInkludert11_17> {
        val vedtaksdata = behandlingsRepository.hentVedtaksData(fnr, periode)

        return vedtaksdata.flatMap {
            it.rettighetsTypeTidslinje
                .segmenter()
                .map { (periode, verdi) ->
                    PeriodeInkludert11_17(
                        no.nav.aap.api.intern.Periode(
                            periode.fom,
                            periode.tom
                        ), aktivitetsfaseNavn = verdi, aktivitetsfaseKode = verdi
                    )
                }
        }
    }
}