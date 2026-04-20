package no.nav.aap.api.kelvin

import java.time.LocalDate
import no.nav.aap.api.intern.DsopRettighetsTypeDTO
import no.nav.aap.api.intern.DsopStatusDTO
import no.nav.aap.api.intern.DsopVedtakDTO
import no.nav.aap.api.intern.DsopVedtaksTypeDTO
import no.nav.aap.api.intern.PeriodeDTO
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode

class DsopService(
    private val behandlingsRepository: BehandlingsRepository,
) {
    constructor(connection: DBConnection): this(
        behandlingsRepository = BehandlingsRepository(connection),
    )

    fun hentDsopVedtak(fnr: String, uttrekksperiode: Periode): List<DsopVedtakDTO> {
        val vedtaksdata = behandlingsRepository.hentVedtaksData(fnr, uttrekksperiode)

        return vedtaksdata.flatMap {
            it.rettighetsTypeTidsLinje
                .somTidslinje({ rettighetsTypePeriode ->
                    Periode(
                        rettighetsTypePeriode.fom,
                        rettighetsTypePeriode.tom
                    )
                }, { rettighetstype -> rettighetstype.verdi })
                .komprimer()
                .segmenter()
                .map { (periode, verdi) -> RettighetsTypePeriode(periode.fom, periode.tom, verdi) }
                .map { rettighetsTypePeriode ->
                    DsopVedtakDTO(
                        vedtakId = it.behandlingsId,
                        vedtakStatus = when (rettighetsTypePeriode.tom >= LocalDate.now()) {
                            true -> DsopStatusDTO.LØPENDE
                            else -> DsopStatusDTO.AVSLUTTET
                        },
                        virkningsperiode = PeriodeDTO(
                            rettighetsTypePeriode.fom,
                            rettighetsTypePeriode.tom
                        ),
                        utfall = "JA",
                        aktivitetsfase = DsopRettighetsTypeDTO.valueOf(rettighetsTypePeriode.verdi),
                        vedtaksType = if (it.nyttVedtak) DsopVedtaksTypeDTO.O else DsopVedtaksTypeDTO.E,
                    )
                }
        }
    }
}