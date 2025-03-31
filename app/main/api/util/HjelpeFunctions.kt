package api.util

import no.nav.aap.api.intern.*
import java.time.LocalDate
import java.time.LocalDateTime


fun no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum.fraKontrakt(): Maksimum {
    return Maksimum(
        vedtak = this.vedtak.map { it.fraKontrakt() }
    )
}

fun no.nav.aap.arenaoppslag.kontrakt.modeller.Vedtak.fraKontrakt(): Vedtak {
    return Vedtak(
        this.dagsats,
        this.vedtaksId,
        this.status,
        this.saksnummer,
        LocalDate.parse(this.vedtaksdato),
        this.periode.fraKontrakt(),
        rettighetsType = this.rettighetsType,
        beregningsgrunnlag = this.beregningsgrunnlag,
        barnMedStonad = this.barnMedStonad,
        vedtaksTypeKode = this.vedtaksTypeKode,
        vedtaksTypeNavn = this.vedtaksTypeNavn,
        utbetaling = this.utbetaling.map { it.fraKontrakt() },
    )
}

fun no.nav.aap.arenaoppslag.kontrakt.modeller.Vedtak.fraKontraktUtenUtbetaling(): VedtakUtenUtbetaling {
    return VedtakUtenUtbetaling(
        this.dagsats,
        this.vedtaksId,
        this.status,
        this.saksnummer,
        LocalDate.parse(this.vedtaksdato),
        periode = this.periode.fraKontrakt(),
        rettighetsType = this.rettighetsType,
        beregningsgrunnlag = this.beregningsgrunnlag,
        barnMedStonad = this.barnMedStonad,
        vedtaksTypeKode = this.vedtaksTypeKode,
        vedtaksTypeNavn = this.vedtaksTypeNavn,
    )
}


fun no.nav.aap.arenaoppslag.kontrakt.modeller.UtbetalingMedMer.fraKontrakt(): UtbetalingMedMer {
    return UtbetalingMedMer(
        this.reduksjon?.fraKontrakt(),
        this.utbetalingsgrad,
        this.periode.fraKontrakt(),
        this.belop,
        this.dagsats,
        this.barnetillegg
    )
}

fun no.nav.aap.arenaoppslag.kontrakt.modeller.Reduksjon.fraKontrakt(): Reduksjon {
    return Reduksjon(
        this.timerArbeidet,
        this.annenReduksjon.fraKontrakt()
            .let { (it.fraver ?: 0F) + (it.sykedager ?: 0F) + it.sentMeldekort }
    )
}

fun no.nav.aap.arenaoppslag.kontrakt.modeller.AnnenReduksjon.fraKontrakt(): AnnenReduksjon {
    return AnnenReduksjon(
        this.sykedager,
        if (this.sentMeldekort == true) 1 else 0,
        this.fraver
    )
}


fun no.nav.aap.arenaoppslag.kontrakt.modeller.Periode.fraKontrakt(): Periode {
    return Periode(fraOgMedDato, tilOgMedDato)
}