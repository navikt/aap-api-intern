package no.nav.aap.api.maksimum

import no.nav.aap.api.intern.Maksimum
import no.nav.aap.api.intern.Medium
import no.nav.aap.api.intern.Vedtak
import no.nav.aap.api.intern.VedtakUtenUtbetaling
import java.time.LocalDate

enum class InternKilde {
    ARENA,
    KELVIN,
}

data class InternPeriode(
    val fraOgMedDato: LocalDate?,
    val tilOgMedDato: LocalDate?,
)

data class InternAnnenReduksjon(
    val sykedager: Float?,
    val sentMeldekort: Int,
    val fravær: Float?,
) {
    fun samletReduksjon(): Float = (fravær ?: 0F) + (sykedager ?: 0F) + sentMeldekort
}

data class InternReduksjon(
    val timerArbeidet: Double,
    val annenReduksjon: InternAnnenReduksjon,
)

data class InternUtbetalingMedMer(
    val reduksjon: InternReduksjon?,
    val utbetalingsgrad: Int?,
    val periode: InternPeriode,
    val belop: Int,
    val dagsats: Int,
    val barnetillegg: Int,
)

data class InternMaksimum(
    val vedtak: List<InternVedtak>,
)

data class InternVedtak(
    val dagsats: Int,
    val dagsatsEtterUføreReduksjon: Int?,
    val vedtakId: String,
    val status: String,
    val saksnummer: String,
    val vedtaksdato: LocalDate,
    val periode: InternPeriode,
    val rettighetsType: String,
    val beregningsgrunnlag: Int,
    val barnMedStonad: Int,
    val barnetillegg: Int,
    val barnetilleggSats: Int,
    val kildesystem: InternKilde,
    val samordningsId: String? = null,
    val opphorsAarsak: String? = null,
    val vedtaksTypeKode: String?,
    val vedtaksTypeNavn: String?,
    val utbetaling: List<InternUtbetalingMedMer>,
    val lopenrvedtak: Int? = null,
    val relatertVedtak: Int? = null,
)

data class InternMedium(
    val vedtak: List<InternVedtakUtenUtbetaling>,
)

data class InternVedtakUtenUtbetaling(
    val dagsats: Int,
    val dagsatsEtterUføreReduksjon: Int?,
    val vedtakId: String,
    val status: String,
    val saksnummer: String,
    val vedtaksdato: LocalDate,
    val vedtaksTypeKode: String?,
    val vedtaksTypeNavn: String?,
    val periode: InternPeriode,
    val rettighetsType: String,
    val beregningsgrunnlag: Int,
    val barnMedStonad: Int,
    val barnetillegg: Int,
    val kildesystem: InternKilde,
    val samordningsId: String? = null,
    val opphorsAarsak: String? = null,
    val lopenrvedtak: Int? = null,
    val relatertVedtak: Int? = null,
)

fun InternMaksimum.tilKontrakt(): Maksimum =
    Maksimum(vedtak = vedtak.map(InternVedtak::tilKontrakt))

fun InternMedium.tilKontrakt(): Medium =
    Medium(vedtak = vedtak.map(InternVedtakUtenUtbetaling::tilKontrakt))

fun InternVedtak.tilKontrakt(): Vedtak =
    Vedtak(
        dagsats = dagsats,
        dagsatsEtterUføreReduksjon = dagsatsEtterUføreReduksjon,
        vedtakId = vedtakId,
        status = status,
        saksnummer = saksnummer,
        vedtaksdato = vedtaksdato,
        periode = periode.tilKontrakt(),
        rettighetsType = rettighetsType,
        beregningsgrunnlag = beregningsgrunnlag,
        barnMedStonad = barnMedStonad,
        barnetillegg = barnetillegg,
        barnetilleggSats = barnetilleggSats,
        kildesystem = kildesystem.tilKontrakt(),
        samordningsId = samordningsId,
        opphorsAarsak = opphorsAarsak,
        vedtaksTypeKode = vedtaksTypeKode,
        vedtaksTypeNavn = vedtaksTypeNavn,
        utbetaling = utbetaling.map(InternUtbetalingMedMer::tilKontrakt),
    )

fun InternVedtakUtenUtbetaling.tilKontrakt(): VedtakUtenUtbetaling =
    VedtakUtenUtbetaling(
        dagsats = dagsats,
        dagsatsEtterUføreReduksjon = dagsatsEtterUføreReduksjon,
        vedtakId = vedtakId,
        status = status,
        saksnummer = saksnummer,
        vedtaksdato = vedtaksdato,
        vedtaksTypeKode = vedtaksTypeKode,
        vedtaksTypeNavn = vedtaksTypeNavn,
        periode = periode.tilKontrakt(),
        rettighetsType = rettighetsType,
        beregningsgrunnlag = beregningsgrunnlag,
        barnMedStonad = barnMedStonad,
        barnetillegg = barnetillegg,
        kildesystem = kildesystem.tilKontrakt(),
        samordningsId = samordningsId,
        opphorsAarsak = opphorsAarsak,
    )

fun InternUtbetalingMedMer.tilKontrakt(): no.nav.aap.api.intern.UtbetalingMedMer =
    no.nav.aap.api.intern.UtbetalingMedMer(
        reduksjon = reduksjon?.tilKontrakt(),
        utbetalingsgrad = utbetalingsgrad,
        periode = periode.tilKontrakt(),
        belop = belop,
        dagsats = dagsats,
        barnetillegg = barnetillegg,
    )

fun InternReduksjon.tilKontrakt(): no.nav.aap.api.intern.Reduksjon =
    no.nav.aap.api.intern.Reduksjon(
        timerArbeidet = timerArbeidet,
        annenReduksjon = annenReduksjon.samletReduksjon(),
    )

fun InternPeriode.tilKontrakt(): no.nav.aap.api.intern.Periode =
    no.nav.aap.api.intern.Periode(
        fraOgMedDato = fraOgMedDato,
        tilOgMedDato = tilOgMedDato,
    )

fun InternKilde.tilKontrakt(): no.nav.aap.api.intern.Kilde =
    when (this) {
        InternKilde.ARENA -> no.nav.aap.api.intern.Kilde.ARENA
        InternKilde.KELVIN -> no.nav.aap.api.intern.Kilde.KELVIN
    }
