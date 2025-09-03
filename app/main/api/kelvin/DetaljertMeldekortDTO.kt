package api.kelvin

import no.nav.aap.api.intern.MeldekortDetalj
import no.nav.aap.api.intern.TimerPåDag
import no.nav.aap.api.intern.Vedtak
import no.nav.aap.komponenter.type.Periode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class MeldekortDetaljListeDTO(
    val personIdent: String,
    val meldekort: List<MeldekortDTO>,
)

data class MeldekortDTO(
    val mottattTidspunkt: LocalDateTime,
    val meldePeriode: Periode,
    val arbeidPerDag: List<MeldeDagDTO>,
) {

    fun tilKontrakt(vedtak: Vedtak): MeldekortDetalj {
        return MeldekortDetalj(
            mottattTidspunkt = this.mottattTidspunkt,
            meldePeriode = no.nav.aap.api.intern.Periode(
                this.meldePeriode.fom, this.meldePeriode.tom
            ),
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

    data class MeldeDagDTO(
        val dag: LocalDate,
        val timerArbeidet: BigDecimal,
    )


}