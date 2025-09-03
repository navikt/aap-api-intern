package api.kelvin

import no.nav.aap.api.intern.MeldekortDetalj
import no.nav.aap.api.intern.TimerPåDag
import no.nav.aap.api.intern.Vedtak
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
) {
    fun tilKontrakt(vedtak: Vedtak): MeldekortDetalj {
        return MeldekortDetalj(
            saksnummer = this.saksnummer,
            mottattTidspunkt = this.mottattTidspunkt,
            meldePeriode = no.nav.aap.api.intern.Periode(this.meldePeriode.fom, this.meldePeriode.fom),
            arbeidPerDag = this.arbeidPerDag.map {
                TimerPåDag(
                    dag = it.dag,
                    timerArbeidet = it.timerArbeidet,
                )
            },
            dagsats = vedtak.dagsats,
            ukesats = vedtak.dagsats * 5,
            opphorsAarsak = vedtak.opphorsAarsak,
            vedtaksdato = vedtak.vedtaksdato,
        )

    }

    data class MeldeDag(
        val dag: LocalDate,
        val timerArbeidet: BigDecimal,
    )

}