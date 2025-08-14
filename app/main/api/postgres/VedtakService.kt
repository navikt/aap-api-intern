package api.postgres

import api.utledVedtakStatus
import no.nav.aap.api.intern.Kilde
import no.nav.aap.api.intern.Maksimum
import no.nav.aap.api.intern.UtbetalingMedMer
import no.nav.aap.api.intern.Vedtak
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

class VedtakService(
    private val behandlingsRepository: BehandlingsRepository,
    private val nå: LocalDate = LocalDate.now()
) {
    fun hentMaksimum(fnr: String, interval: Periode): Maksimum {
        val kelvinData = behandlingsRepository.hentVedtaksData(fnr, interval)
        val vedtak = kelvinData.flatMap { behandling ->
            val rettighetsTypeTidslinje = Tidslinje(
                behandling.rettighetsTypeTidsLinje.map {
                    Segment(
                        Periode(it.fom, it.tom),
                        it.verdi
                    )
                }
            )

            val tilkjent = Tidslinje(
                behandling.tilkjent.map {
                    Segment(
                        Periode(it.tilkjentFom, it.tilkjentTom),
                        TilkjentDB(
                            it.dagsats,
                            it.grunnlag,
                            it.gradering,
                            it.grunnlagsfaktor,
                            it.grunnbeløp,
                            it.antallBarn,
                            it.barnetilleggsats,
                            it.barnetillegg,
                            it.samordningUføregradering
                        )
                    )
                }
            )

            val perioderTidslinje = rettighetsTypeTidslinje.kombiner(
                tilkjent,
                JoinStyle.LEFT_JOIN { periode, left, right ->
                    Segment(
                        periode,
                        VedtakUtenUtbetalingUtenPeriode(
                            vedtakId = behandling.vedtakId.toString(),
                            dagsats = right?.verdi?.dagsats ?: 0,
                            status = utledVedtakStatus(
                                behandling.behandlingStatus,
                                behandling.sak.status,
                                periode
                            ),
                            saksnummer = behandling.sak.saksnummer,
                            vedtaksdato = behandling.vedtaksDato,
                            rettighetsType = left.verdi,
                            beregningsgrunnlag = right?.verdi?.grunnlag?.toInt() ?: 0,
                            barnMedStonad = right?.verdi?.antallBarn ?: 0,
                            kildesystem = Kilde.KELVIN.toString(),
                            samordningsId = behandling.samId,
                            opphorsAarsak = null
                        )
                    )
                }
            ).komprimer()
            val tilkjentPerioder =
                tilkjent.splittOppIPerioder(perioderTidslinje.perioder().toList())

            perioderTidslinje.kombiner(
                tilkjentPerioder,
                JoinStyle.LEFT_JOIN { periode, left, right ->
                    Segment(
                        periode,
                        Vedtak(
                            vedtakId = left.verdi.vedtakId,
                            dagsats = left.verdi.dagsats,
                            status = left.verdi.status,
                            saksnummer = left.verdi.saksnummer,
                            vedtaksdato = left.verdi.vedtaksdato,
                            periode = no.nav.aap.api.intern.Periode(periode.fom, periode.tom),
                            rettighetsType = left.verdi.rettighetsType,
                            // TODO: bør bruke felles logikk her
                            beregningsgrunnlag = left.verdi.beregningsgrunnlag * 260, //GANGER MED 260 FOR Å FÅ ÅRLIG SUM
                            barnMedStonad = left.verdi.barnMedStonad,
                            vedtaksTypeKode = null,
                            vedtaksTypeNavn = null,
                            utbetaling = right?.verdi?.filter {
                                it.periode.tom.isBefore(nå) || it.periode.tom.isEqual(nå)
                            }?.map { utbetaling ->
                                UtbetalingMedMer(
                                    reduksjon = null,
                                    utbetalingsgrad = utbetaling.verdi.gradering,
                                    periode = no.nav.aap.api.intern.Periode(
                                        utbetaling.periode.fom,
                                        utbetaling.periode.tom
                                    ),
                                    // TODO: bør hente korrekt beløp på samme måte som i behandlingsflyt
                                    belop = ((utbetaling.verdi.dagsats + utbetaling.verdi.barnetillegg.toInt()) * utbetaling.verdi.gradering) / 100 * weekdaysBetween(
                                        utbetaling.periode.fom,
                                        utbetaling.periode.tom
                                    ),
                                    dagsats = utbetaling.verdi.dagsats * utbetaling.verdi.gradering / 100,
                                    barnetilegg = utbetaling.verdi.barnetillegg.toInt(),
                                    barnetillegg = utbetaling.verdi.barnetillegg.toInt(),
                                )
                            } ?: emptyList(),
                            kildesystem = Kilde.valueOf(left.verdi.kildesystem),
                            samordningsId = left.verdi.samordningsId,
                            opphorsAarsak = left.verdi.opphorsAarsak
                        )
                    )
                }
            ).komprimer().map { it.verdi }
                .filter { it.status == Status.LØPENDE.toString() || it.status == Status.AVSLUTTET.toString() }


        }

        return Maksimum(vedtak)
    }
}