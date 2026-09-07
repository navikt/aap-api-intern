package no.nav.aap.api.kelvin

import no.nav.aap.komponenter.type.Periode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class Meldekort(
    val personIdent: String,
    val saksnummer: String,
    val behandlingId: Long,
    val journalpostId: String? = null,
    val mottattTidspunkt: LocalDateTime,
    val meldePeriode: Periode,
    val arbeidPerDag: List<MeldeDag>,
) {

    data class MeldeDag(
        val dag: LocalDate,
        val timerArbeidet: BigDecimal,
    )
}