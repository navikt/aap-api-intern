package no.nav.aap.api.kelvin

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.aap.api.intern.MeldekortDetalj
import no.nav.aap.api.intern.TimerPaaDag
import no.nav.aap.api.intern.VedtakUtenUtbetaling
import no.nav.aap.komponenter.type.Periode

data class Meldekort(
    val personIdent: String,
    val saksnummer: String,
    val behandlingId: Long,
    val mottattTidspunkt: LocalDateTime,
    val meldePeriode: Periode,
    val arbeidPerDag: List<MeldeDag>,
    val meldepliktStatusKode: String?,
    val rettighetsTypeKode: String?
) {
    fun tilKontrakt(vedtak: VedtakUtenUtbetaling?): MeldekortDetalj {
        return MeldekortDetalj(
            saksnummer = this.saksnummer,
            mottattTidspunkt = this.mottattTidspunkt,
            meldePeriode = no.nav.aap.api.intern.Periode(this.meldePeriode.fom, this.meldePeriode.tom),
            arbeidPerDag = this.arbeidPerDag.map {
                TimerPaaDag(
                    dag = it.dag,
                    timerArbeidet = it.timerArbeidet,
                )
            },
            dagsats = vedtak?.dagsats,
            ukesats = vedtak?.dagsats?.times(5),
            vedtaksdato = vedtak?.vedtaksdato,
        )
    }

    data class MeldeDag(
        val dag: LocalDate,
        val timerArbeidet: BigDecimal,
    )
}