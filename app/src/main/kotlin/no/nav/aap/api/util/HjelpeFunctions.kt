package no.nav.aap.api.util

import no.nav.aap.api.maksimum.InternAnnenReduksjon
import no.nav.aap.api.maksimum.InternKilde
import no.nav.aap.api.maksimum.InternMaksimum
import no.nav.aap.api.maksimum.InternPeriode
import no.nav.aap.api.maksimum.InternReduksjon
import no.nav.aap.api.maksimum.InternUtbetalingMedMer
import no.nav.aap.api.maksimum.InternVedtak
import no.nav.aap.api.maksimum.InternVedtakUtenUtbetaling
import no.nav.aap.arenaoppslag.kontrakt.modeller.Vedtak
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException

private val logger = LoggerFactory.getLogger("api.util.HjelpeFunctions")

fun no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum.fraKontrakt(): InternMaksimum {
    return InternMaksimum(
        vedtak = this.vedtak.map { it.fraKontrakt() })
}

fun Vedtak.fraKontrakt(): InternVedtak {
    return InternVedtak(
        dagsats = this.dagsats,
        dagsatsEtterUføreReduksjon = null,
        vedtakId = this.vedtaksId,
        status = this.status,
        saksnummer = this.saksnummer,
        vedtaksdato = localDate(this.vedtaksdato),
        periode = this.periode.fraKontrakt(),
        rettighetsType = this.rettighetsType,
        beregningsgrunnlag = this.beregningsgrunnlag,
        barnMedStonad = this.barnMedStonad,
        vedtaksTypeKode = this.vedtaksTypeKode,
        vedtaksTypeNavn = this.vedtaksTypeNavn,
        utbetaling = this.utbetaling.map { it.fraKontrakt() },
        kildesystem = InternKilde.ARENA,
        barnetillegg = this.utbetaling.lastOrNull()?.barnetillegg ?: 0,
        barnetilleggSats = 0, // Hva skal dette være? Kanskje (this.utbetaling.lastOrNull()?.barnetillegg ?: 0) / this.barnMedStonad,
        opphorsAarsak = null,
        lopenrvedtak = this.lopenrvedtak,
        relatertVedtak = this.relatertVedtak,
        samIdOgTpnr = emptyList()
    )
}

fun Vedtak.fraKontraktUtenUtbetaling(): InternVedtakUtenUtbetaling {
    return InternVedtakUtenUtbetaling(
        dagsats = this.dagsats,
        dagsatsEtterUføreReduksjon = null,
        vedtakId = this.vedtaksId,
        status = this.status,
        saksnummer = this.saksnummer,
        vedtaksdato = localDate(this.vedtaksdato),
        periode = this.periode.fraKontrakt(),
        rettighetsType = this.rettighetsType,
        beregningsgrunnlag = this.beregningsgrunnlag,
        barnMedStonad = this.barnMedStonad,
        vedtaksTypeKode = this.vedtaksTypeKode,
        vedtaksTypeNavn = this.vedtaksTypeNavn,
        barnetillegg = this.utbetaling.lastOrNull()?.barnetillegg ?: 0,
        kildesystem = InternKilde.ARENA,
        samIdOgTpnr = emptyList(),
        opphorsAarsak = null,
        lopenrvedtak = this.lopenrvedtak,
        relatertVedtak = this.relatertVedtak,
    )
}

/**
 * Midlertidig, kopiert fra api-appen. Trenger ikke denne når kontrakt er ordentlig typet.
 */
fun localDate(s: String): LocalDate {
    val formatter = DateTimeFormatterBuilder()
        .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        .toFormatter()

    return try {
        LocalDate.parse(s, formatter)
    } catch (e: DateTimeParseException) {
        logger.error("Failed to parse date string: $s", e)
        throw e
    }
}


fun no.nav.aap.arenaoppslag.kontrakt.modeller.UtbetalingMedMer.fraKontrakt(): InternUtbetalingMedMer {
    return InternUtbetalingMedMer(
        reduksjon = this.reduksjon?.fraKontrakt(),
        utbetalingsgrad = this.utbetalingsgrad,
        periode = this.periode.fraKontrakt(),
        belop = this.belop,
        dagsats = this.dagsats,
        barnetillegg = this.barnetillegg,
    )
}

fun no.nav.aap.arenaoppslag.kontrakt.modeller.Reduksjon.fraKontrakt(): InternReduksjon {
    return InternReduksjon(
        this.timerArbeidet,
        this.annenReduksjon.fraKontrakt()
    )
}

fun no.nav.aap.arenaoppslag.kontrakt.modeller.AnnenReduksjon.fraKontrakt(): InternAnnenReduksjon {
    return InternAnnenReduksjon(
        sykedager = this.sykedager,
        sentMeldekort = if (this.sentMeldekort == true) 1 else 0,
        fravær = this.fraver
    )
}


fun no.nav.aap.arenaoppslag.kontrakt.modeller.Periode.fraKontrakt(): InternPeriode {
    return InternPeriode(fraOgMedDato, tilOgMedDato)
}