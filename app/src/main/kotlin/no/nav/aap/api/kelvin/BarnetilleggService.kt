package no.nav.aap.api.kelvin

import no.nav.aap.api.intern.BisysBarnMedBarnetillegg
import no.nav.aap.api.intern.BisysBarnetilleggResponse
import no.nav.aap.api.intern.BisysPeriodeMedBeløp
import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.Clock
import java.time.LocalDate

class BarnetilleggService(
    connection: DBConnection,
    private val pdlGateway: IPdlGateway,
    private val clock: Clock,
) {
    private val behandlingsRepository = BehandlingsRepository(connection)

    fun hentBarnetillegg(
        personIdentifikator: String,
    ): BisysBarnetilleggResponse {
        val personIdenter =
            pdlGateway.hentAlleIdenterForPerson(personIdentifikator).map { it.ident }
                .ifEmpty { return BisysBarnetilleggResponse(emptyList()) }

        val søkeperiode = Periode(Tid.MIN, LocalDate.now(clock))

        val behandling =
            personIdenter.flatMap { behandlingsRepository.hentVedtaksData(it, søkeperiode) }
                .maxByOrNull { it.vedtakId }
                ?: return BisysBarnetilleggResponse(emptyList())

        val barnMedBarnetillegg = behandling.barnMedBarnetillegg.map { barn ->
            BisysBarnMedBarnetillegg(
                ident = barn.ident,
                perioderMedBarnetillegg = barn.perioderMedBarnetillegg.map {
                    BisysPeriodeMedBeløp(fra = it.fom, til = it.tom, beløp = it.beløp)
                },
            )
        }

        return BisysBarnetilleggResponse(barnMedBarnetillegg)
    }
}
