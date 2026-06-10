package no.nav.aap.api.kelvin

import no.nav.aap.api.intern.MeldekortDetalj
import no.nav.aap.api.intern.TimerPaaDag
import no.nav.aap.api.intern.Utbetaling
import no.nav.aap.komponenter.type.Periode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class Meldekort(
    val personIdent: String,
    val saksnummer: String,
    val behandlingId: Long,
    val mottattTidspunkt: LocalDateTime,
    val meldePeriode: Periode,
    val arbeidPerDag: List<MeldeDag>,
) {
    fun tilKontrakt(tilkjentYtelsePerioder: List<Pair<Periode, TilkjentYtelse>>): MeldekortDetalj {
        return MeldekortDetalj(
            saksnummer = this.saksnummer,
            mottattTidspunkt = this.mottattTidspunkt,
            meldePeriode = no.nav.aap.api.intern.Periode(
                this.meldePeriode.fom,
                this.meldePeriode.tom
            ),
            arbeidPerDag = this.arbeidPerDag.map {
                TimerPaaDag(
                    dag = it.dag,
                    timerArbeidet = it.timerArbeidet,
                )
            },
            dagsats = null,
            ukesats = null,
            vedtaksdato = null,
            belop = tilkjentYtelsePerioder.map { (periode, tilkjentYtelse) ->
                tilkjentYtelse.dagsats * weekdaysBetween(
                    periode.fom,
                    periode.tom
                )
            }.sum(),
            utbetalinger = tilkjentYtelsePerioder.map { (periode, tilkjentYtelse) ->
                Utbetaling(
                    fraDato = periode.fom,
                    tilDato = periode.tom,
                    utbetalingsgrad = tilkjentYtelse.gradering,
                    dagsats = tilkjentYtelse.dagsats,
                )
            }
        )
    }

    data class MeldeDag(
        val dag: LocalDate,
        val timerArbeidet: BigDecimal,
    )
}