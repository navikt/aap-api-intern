package no.nav.aap.api.kelvin

import no.nav.aap.api.intern.BisysBarnMedBarnetillegg
import no.nav.aap.api.intern.BisysBarnetilleggResponse
import no.nav.aap.api.intern.BisysPeriodeMedBeløp
import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate

class BarnetilleggService(
    connection: DBConnection,
    private val pdlGateway: IPdlGateway,
    private val clock: Clock,
) {
    private val behandlingsRepository = BehandlingsRepository(connection)

    private val log = LoggerFactory.getLogger(javaClass)

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
                    if (it.uredusertBeløp == null || it.sats == null) {
                        log.warn("Sats eller full sats er null, for behandling ${behandling.behandlingsReferanse}. Sats: ${it.sats}. Uredusert beløp: ${it.uredusertBeløp}")
                    }
                    BisysPeriodeMedBeløp(
                        fra = it.fom,
                        til = it.tom,
                        beløp = it.beløp,
                        sats = it.sats ?: BigDecimal.ZERO,
                        uredusertBeløp = it.uredusertBeløp ?: BigDecimal.ZERO,
                    )
                },
            )
        }

        return BisysBarnetilleggResponse(barnMedBarnetillegg)
    }
}
