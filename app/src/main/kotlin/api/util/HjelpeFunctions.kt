package api.util

import no.nav.aap.api.intern.*
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException


fun no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum.fraKontrakt(): Maksimum {
    return Maksimum(
        vedtak = this.vedtak.map { it.fraKontrakt() })
}

fun no.nav.aap.arenaoppslag.kontrakt.modeller.Vedtak.fraKontrakt(): Vedtak {
    return Vedtak(
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
        barnetillegg = this.utbetaling.lastOrNull()?.barnetillegg ?: 0
    )
}

fun no.nav.aap.arenaoppslag.kontrakt.modeller.Vedtak.fraKontraktUtenUtbetaling(): VedtakUtenUtbetaling {
    return VedtakUtenUtbetaling(
        this.dagsats,
        null,
        this.vedtaksId,
        this.status,
        this.saksnummer,
        localDate(this.vedtaksdato),
        periode = this.periode.fraKontrakt(),
        rettighetsType = this.rettighetsType,
        beregningsgrunnlag = this.beregningsgrunnlag,
        barnMedStonad = this.barnMedStonad,
        vedtaksTypeKode = this.vedtaksTypeKode,
        vedtaksTypeNavn = this.vedtaksTypeNavn,
        barnetillegg = this.utbetaling.lastOrNull()?.barnetillegg ?: 0,
        kildesystem = "ARENA"
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


fun no.nav.aap.arenaoppslag.kontrakt.modeller.UtbetalingMedMer.fraKontrakt(): UtbetalingMedMer {
    return UtbetalingMedMer(
        this.reduksjon?.fraKontrakt(),
        this.utbetalingsgrad,
        this.periode.fraKontrakt(),
        this.belop,
        this.dagsats,
        this.barnetillegg,
        this.barnetillegg
    )
}

fun no.nav.aap.arenaoppslag.kontrakt.modeller.Reduksjon.fraKontrakt(): Reduksjon {
    return Reduksjon(
        this.timerArbeidet,
        this.annenReduksjon.fraKontrakt()
            .let { (it.fraver ?: 0F) + (it.sykedager ?: 0F) + it.sentMeldekort })
}

fun no.nav.aap.arenaoppslag.kontrakt.modeller.AnnenReduksjon.fraKontrakt(): AnnenReduksjon {
    return AnnenReduksjon(
        this.sykedager, if (this.sentMeldekort == true) 1 else 0, this.fraver
    )
}


fun no.nav.aap.arenaoppslag.kontrakt.modeller.Periode.fraKontrakt(): Periode {
    return Periode(fraOgMedDato, tilOgMedDato)
}