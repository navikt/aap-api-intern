package no.nav.aap.api.intern

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

public data class MeldekortDetaljerResponse(
    val personIdent: String,
    val meldekort: List<MeldekortDetalj>,
)

public data class MeldekortDetalj(
    val saksnummer: String,
    val mottattTidspunkt: LocalDateTime,
    val meldePeriode: Periode,
    val arbeidPerDag: List<TimerPaaDag>,
    @property:Deprecated("Ikke i bruk, vil fjernes.")
    val dagsats: Int?,
    @property:Deprecated("Ikke i bruk, vil fjernes.")
    val ukesats: Int?,
    val vedtaksdato: LocalDate?,
)

public data class TimerPaaDag(
    val dag: LocalDate,
    val timerArbeidet: BigDecimal,
)
