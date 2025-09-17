package api.kelvin

import no.nav.aap.api.intern.MeldekortDetalj
import no.nav.aap.api.intern.TimerPaaDag
import no.nav.aap.api.intern.VedtakUtenUtbetaling
import no.nav.aap.komponenter.type.Periode

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class MeldekortDTO(
    val personIdent: String,
    val saksnummer: String,
    val mottattTidspunkt: LocalDateTime,
    val meldePeriode: Periode,
    val arbeidPerDag: List<MeldeDag>,
    val meldepliktStatusKode: String?,
    val rettighetsTypeKode: String?,
    val avslagsårsakKode: String?,
) {
    fun tilKontrakt(vedtak: VedtakUtenUtbetaling?): MeldekortDetalj {
        return MeldekortDetalj(
            saksnummer = this.saksnummer,
            mottattTidspunkt = this.mottattTidspunkt,
            meldePeriode = no.nav.aap.api.intern.Periode(this.meldePeriode.fom, this.meldePeriode.fom),
            arbeidPerDag = this.arbeidPerDag.map {
                TimerPaaDag(
                    dag = it.dag,
                    timerArbeidet = it.timerArbeidet,
                )
            },
            dagsats = vedtak?.dagsats,
            ukesats = vedtak?.dagsats?.times(5),
            opphorsAarsak = vedtak?.opphorsAarsak,
            vedtaksdato = vedtak?.vedtaksdato,
        )

    }

    data class MeldeDag(
        val dag: LocalDate,
        val timerArbeidet: BigDecimal,
    )

}