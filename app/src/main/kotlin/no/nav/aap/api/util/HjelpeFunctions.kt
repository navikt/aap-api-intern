package no.nav.aap.api.util

import no.nav.aap.api.intern.*
import no.nav.aap.api.maksimum.InternAnnenReduksjon
import no.nav.aap.api.maksimum.InternKilde
import no.nav.aap.api.maksimum.InternMaksimum
import no.nav.aap.api.maksimum.InternPeriode
import no.nav.aap.api.maksimum.InternReduksjon
import no.nav.aap.api.maksimum.InternUtbetalingMedMer
import no.nav.aap.api.maksimum.InternVedtak
import no.nav.aap.api.maksimum.InternVedtakUtenUtbetaling
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException


fun no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum.fraKontrakt(): InternMaksimum {
    return InternMaksimum(
        vedtak = this.vedtak.map { it.fraKontrakt() })
}

fun no.nav.aap.arenaoppslag.kontrakt.modeller.Vedtak.fraKontrakt(): InternVedtak {
    return InternVedtak(
        this.dagsats,
        null,
        this.vedtaksId,
        this.status,
        this.saksnummer,
        localDate(this.vedtaksdato),
        this.periode.fraKontrakt(),
        rettighetsType = this.rettighetsType,
        beregningsgrunnlag = this.beregningsgrunnlag,
        barnMedStonad = this.barnMedStonad,
        vedtaksTypeKode = this.vedtaksTypeKode,
        vedtaksTypeNavn = this.vedtaksTypeNavn,
        utbetaling = this.utbetaling.map { it.fraKontrakt() },
        kildesystem = InternKilde.ARENA,
        barnetillegg = this.utbetaling.lastOrNull()?.barnetillegg ?: 0,
        barnetilleggSats = 0, // Hva skal dette være? Kanskje (this.utbetaling.lastOrNull()?.barnetillegg ?: 0) / this.barnMedStonad,
        samordningsId = null,
        opphorsAarsak = null,
        lopenrvedtak = this.lopenrvedtak,
        relatertVedtak = this.relatertVedtak,
    )
}

fun no.nav.aap.arenaoppslag.kontrakt.modeller.Vedtak.fraKontraktUtenUtbetaling(): InternVedtakUtenUtbetaling {
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
        samordningsId = null,
        opphorsAarsak = null,
        lopenrvedtak = this.lopenrvedtak,
        relatertVedtak = this.relatertVedtak,
    )
}

private val logger = LoggerFactory.getLogger("api.util.HjelpeFunctions")

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

fun localDateTime(s: String): LocalDateTime? {
    val formatter = DateTimeFormatterBuilder()
        .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        .toFormatter()

    return try {
        LocalDateTime.parse(s, formatter)
    } catch (e: DateTimeParseException) {
        logger.error("Failed to parse date string: $s", e)
        throw e
    }
}


fun no.nav.aap.arenaoppslag.kontrakt.modeller.UtbetalingMedMer.fraKontrakt(): InternUtbetalingMedMer {
    return InternUtbetalingMedMer(
        this.reduksjon?.fraKontrakt(),
        this.utbetalingsgrad,
        this.periode.fraKontrakt(),
        this.belop,
        this.dagsats,
        this.barnetillegg,
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
        this.sykedager, if (this.sentMeldekort == true) 1 else 0, this.fraver
    )
}


fun no.nav.aap.arenaoppslag.kontrakt.modeller.Periode.fraKontrakt(): InternPeriode {
    return InternPeriode(fraOgMedDato, tilOgMedDato)
}