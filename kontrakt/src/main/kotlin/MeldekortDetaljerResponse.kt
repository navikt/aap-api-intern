package no.nav.aap.api.intern

import com.papsign.ktor.openapigen.annotations.properties.description.Description
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
    @property:Description("Hvilken periode det skrives timer for. Dette vil være to uker før nyeste utbetaling.") val meldePeriode: Periode,
    val arbeidPerDag: List<TimerPaaDag>,
    @property:Deprecated("Ikke i bruk, vil fjernes.") val dagsats: Int?,
    @property:Deprecated("Ikke i bruk, vil fjernes.") val ukesats: Int?,
    val vedtaksdato: LocalDate?,
    @property:Description("Beregnet totalt beløp i meldeperioden.")
    val belop: Int,
    @property:Description("Liste med beregnede utbetalinger. Vil ha flere elementer om noen av verdiene er endrede i meldeperioden.")
    val utbetalinger: List<Utbetaling>,
)

public data class TimerPaaDag(
    val dag: LocalDate,
    val timerArbeidet: BigDecimal,
)

public data class Utbetaling(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val utbetalingsgrad: Int,
    val dagsats: Int,
    val belop: Int
)