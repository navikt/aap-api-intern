package api.kelvin

import java.sql.Date
import java.time.LocalDateTime

data class DetaljertMeldekortListeDTO (
    val personIdent: String,
    val meldeperioder: List<meldekortDTO>,
)

data class meldekortDTO(
    val innsendtTidspunkt: LocalDateTime,
    val meldekortDager: List<MeldeDagDTO>,
    val aarsak_til_opprettelse: String,
)

data class MeldeDagDTO(
    val date: Date,
    val timer_arbeidet: Double,
)