package no.nav.aap.api.intern

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

public data class MeldekortDetaljerResponse(
    val personIdent: String,
    val meldekort: List<MeldekortDetalj>,
)

public data class MeldekortDetalj(
    val mottattTidspunkt: LocalDateTime,
    val meldePeriode: Periode,
    val arbeidPerDag: List<TimerPåDag>,
    var dagsats: Int?,
    var ukesats: Int?,
    var opphorsAarsak: String?,
    var vedtaksdato: LocalDate?,
)

public data class TimerPåDag(
    val dag: LocalDate,
    val timerArbeidet: BigDecimal,
)
