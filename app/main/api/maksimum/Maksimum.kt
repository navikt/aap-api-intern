package api.maksimum

import no.nav.aap.api.intern.Kilde
import no.nav.aap.api.intern.Periode
import java.time.LocalDate
import no.nav.aap.arenaoppslag.kontrakt.modeller.Maksimum as KontraktMaksimum


data class Maksimum(
    val vedtak: List<Vedtak>,
)

data class Medium(val vedtak: List<VedtakUtenUtbetaling>)

fun KontraktMaksimum.fraKontrakt(): Maksimum {
    return Maksimum(
        vedtak = this.vedtak.map { it.fraKontrakt() }
    )
}

data class KelvinPeriode(
    val fom: LocalDate,
    val tom: LocalDate
)

data class VedtakDataKelvin(
    val fnr: String,
    val maksimum: Maksimum
)

/**
 * @param status Hypotese, vedtaksstatuskode
 * @param saksnummer hypotese sak_id
 */
data class Vedtak(
    val vedtaksId: String,
    val dagsats: Int,
    val status: String, //Hypotese, vedtaksstatuskode
    val saksnummer: String,
    val vedtaksdato: String, //reg_dato
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
) {
    fun fraKontraktUtenUtbetaling() {
        TODO("Not yet implemented")
    }
}

data class VedtakUtenUtbetaling(
    val vedtaksId: String,
    val dagsats: Int,
    val status: String, //Hypotese, vedtaksstatuskode
    val saksnummer: String,
    val vedtaksdato: String, //reg_dato
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

fun no.nav.aap.arenaoppslag.kontrakt.modeller.Vedtak.fraKontrakt(): Vedtak {
    return Vedtak(
        this.vedtaksId,
        this.dagsats,
        this.status,
        this.saksnummer,
        this.vedtaksdato,
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
        this.vedtaksId,
        this.dagsats,
        this.status,
        this.saksnummer,
        this.vedtaksdato,
        periode = this.periode.fraKontrakt(),
        rettighetsType = this.rettighetsType,
        beregningsgrunnlag = this.beregningsgrunnlag,
        barnMedStonad = this.barnMedStonad,
        vedtaksTypeKode = this.vedtaksTypeKode,
        vedtaksTypeNavn = this.vedtaksTypeNavn,
    )
}


data class UtbetalingMedMer(
    val reduksjon: Reduksjon? = null,
    val utbetalingsgrad: Int? = null,
    val periode: Periode,
    val belop: Int,
    val dagsats: Int,
    val barnetilegg: Int,
)

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

data class Reduksjon(
    val timerArbeidet: Double,
    val annenReduksjon: Float
)


fun no.nav.aap.arenaoppslag.kontrakt.modeller.Reduksjon.fraKontrakt(): Reduksjon {
    return Reduksjon(
        this.timerArbeidet,
        this.annenReduksjon.fraKontrakt()
            .let { (it.fraver ?: 0F) + (it.sykedager ?: 0F) + it.sentMeldekort }
    )
}


data class AnnenReduksjon(
    val sykedager: Float?,
    val sentMeldekort: Int,
    val fraver: Float?
)

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