package no.nav.aap.api.intern

import java.time.LocalDate

data class Maksimum(val vedtak: List<Vedtak>)

data class Medium(val vedtak: List<VedtakUtenUtbetaling>)

data class InternVedtakRequestApiIntern(
    val personidentifikator: String,
    val fraOgMedDato: LocalDate? = LocalDate.of(1, 1, 1),
    val tilOgMedDato: LocalDate? = LocalDate.of(9999, 12, 31)
)

/**
 * @param status Hypotese, vedtaksstatuskode
 * @param saksnummer hypotese sak_id
 */
data class Vedtak(
    val dagsats: Int,
    val dagsatsEtterUføreReduksjon: Int?,
    val vedtakId: String,
    val status: String,
    val saksnummer: String,
    val vedtaksdato: LocalDate, //reg_dato
    val periode: Periode,
    val rettighetsType: String, ////aktivitetsfase //Aktfasekode
    val beregningsgrunnlag: Int,
    val barnMedStonad: Int,
    val barnetillegg: Int,
    val kildesystem: Kilde = Kilde.ARENA,
    val samordningsId: String? = null,
    val opphorsAarsak: String? = null,
    val vedtaksTypeKode: String?,
    val vedtaksTypeNavn: String?,
    val utbetaling: List<UtbetalingMedMer>,
)

data class VedtakUtenUtbetaling(
    val dagsats: Int,
    val dagsatsEtterUføreReduksjon: Int?,
    val vedtakId: String,
    val status: String, //Hypotese, vedtaksstatuskode
    val saksnummer: String,
    val vedtaksdato: LocalDate, //reg_dato
    val vedtaksTypeKode: String?,
    val vedtaksTypeNavn: String?,
    val periode: Periode,
    val rettighetsType: String, ////aktivitetsfase //Aktfasekode
    val beregningsgrunnlag: Int,
    val barnMedStonad: Int,
    val barnetillegg: Int,
    val kildesystem: String = "ARENA",
    val samordningsId: String? = null,
    val opphorsAarsak: String? = null,
)

/**
 * @param barnetilegg Hvor mange kroner i barnetillegg.
 */
data class UtbetalingMedMer(
    val reduksjon: Reduksjon? = null,
    val utbetalingsgrad: Int? = null,
    val periode: Periode,
    val belop: Int,
    val dagsats: Int,
    @Deprecated("Bruk barnetillegg")
    val barnetilegg: Int,
    val barnetillegg: Int,
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
