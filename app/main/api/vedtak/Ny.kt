package api.vedtak

import api.vedtak.dagens.Periode
import api.vedtak.dagens.Vedtak
import java.time.LocalDate

// Basert på samtaler med Jens-Otto i FP

data class VedtakRequest(
    val ident: String,
    val periode: Periode
)

data class VedtakResponse(
    val vedtakListe: List<Vedtak>
)

data class Vedtak(
    val saksnummer: String,
    val vedtaksdato: LocalDate?,
    val status: Status,
    val periode: Periode,
    val utbetalinger: List<Utbetaling>,
    val dagsats: Double
)

data class Utbetaling(
    val periode: Periode,
    val dagsats: Double,
    val beløp: Double,
    val utbetalingsgrad: Double,
    val barnetillegg: Double
)

enum class Status {
    UNDER_BEHANDLING,
    LØPENDE,
    AVSLUTTET,
    UKJENT
}

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate
)