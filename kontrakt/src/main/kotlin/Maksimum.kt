package no.nav.aap.api.intern

import java.time.LocalDate
import java.time.LocalDateTime

data class Maksimum(val vedtak: List<Vedtak>)

data class Medium(val vedtak: List<VedtakUtenUtbetaling>)

/**
 * @param status Hypotese, vedtaksstatuskode
 * @param saksnummer hypotese sak_id
 */
data class Vedtak(
    val dagsats: Int,
    val vedtakId: String,
    val status: String, //Hypotese, vedtaksstatuskode
    val saksnummer: String,
    val vedtaksdato: LocalDate, //reg_dato
    val periode: Periode,
    val rettighetsType: String, ////aktivitetsfase //Aktfasekode
    val beregningsgrunnlag: Int,
    val barnMedStonad: Int,
    val kildesystem: Kilde = Kilde.ARENA,
    val samordningsId: String? = null,
    val opphorsAarsak: String? = null,
    val vedtaksTypeKode: String,
    val vedtaksTypeNavn: String,
    val utbetaling: List<UtbetalingMedMer>,
)

data class VedtakUtenUtbetaling(
    val dagsats: Int,
    val vedtakId: String,
    val status: String, //Hypotese, vedtaksstatuskode
    val saksnummer: String,
    val vedtaksdato: LocalDate, //reg_dato
    val vedtaksTypeKode: String,
    val vedtaksTypeNavn: String,
    val periode: Periode,
    val rettighetsType: String, ////aktivitetsfase //Aktfasekode
    val beregningsgrunnlag: Int,
    val barnMedStonad: Int,
    val kildesystem: String = "ARENA",
    val samordningsId: String? = null,
    val opphorsAarsak: String? = null,
)

data class UtbetalingMedMer(
    val reduksjon: Reduksjon? = null,
    val utbetalingsgrad: Int? = null,
    val periode: Periode,
    val belop: Int,
    val dagsats: Int,
    val barnetilegg: Int,
)

data class Reduksjon(
    val timerArbeidet: Double,
    val annenReduksjon: Float
)

data class AnnenReduksjon(
    val sykedager: Float?,
    val sentMeldekort: Int,
    val fraver: Float?
)
