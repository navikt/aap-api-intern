package api.kelvin

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class MeldekortDetaljListeDTO (
    val personIdent: String,
    val meldeperioder: List<MeldekortDTO>,
)

data class MeldekortDTO(
    val mottattTidspunkt: LocalDateTime,
    val arbeidPerDag: List<MeldeDagDTO>,
)

data class MeldeDagDTO(
    val dag: LocalDate,
    val timerArbeidet: BigDecimal,
)