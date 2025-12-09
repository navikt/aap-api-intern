package no.nav.aap.api.postgres

import no.nav.aap.api.intern.*
import no.nav.aap.api.utledVedtakStatus
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.Clock
import java.time.LocalDate

class VedtakService(
    private val behandlingsRepository: BehandlingsRepository,
    private val clock: Clock = Clock.systemDefaultZone()
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
                            dagsats = it.dagsats,
                            gradering = it.gradering,
                            grunnlagsfaktor = it.grunnlagsfaktor,
                            grunnbeløp = it.grunnbeløp,
                            antallBarn = it.antallBarn,
                            barnetilleggsats = it.barnetilleggsats,
                            barnetillegg = it.barnetillegg,
                            uføregrad = it.samordningUføregradering
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
                            dagsatsEtterUføreReduksjon = right?.verdi?.regnUtDagsatsEtterUføreReduksjon()
                                ?: 0,
                            status = utledVedtakStatus(
                                behandling.behandlingStatus,
                                behandling.sak.status,
                                periode
                            ),
                            saksnummer = behandling.sak.saksnummer,
                            vedtaksdato = behandling.vedtaksDato,
                            rettighetsType = left.verdi,
                            beregningsgrunnlag = behandling.beregningsgrunnlag.toInt(),
                            barnMedStonad = right?.verdi?.antallBarn ?: 0,
                            kildesystem = Kilde.KELVIN.toString(),
                            samordningsId = behandling.samId,
                            opphorsAarsak = null,
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
                            dagsatsEtterUføreReduksjon = left.verdi.dagsatsEtterUføreReduksjon,
                            status = left.verdi.status,
                            saksnummer = left.verdi.saksnummer,
                            vedtaksdato = left.verdi.vedtaksdato,
                            periode = no.nav.aap.api.intern.Periode(periode.fom, periode.tom),
                            rettighetsType = left.verdi.rettighetsType,
                            beregningsgrunnlag = left.verdi.beregningsgrunnlag,
                            barnMedStonad = left.verdi.barnMedStonad,
                            barnetillegg = left.verdi.barnMedStonad * (right?.verdi?.segmenter()
                                ?.first()?.verdi?.barnetilleggsats?.toInt()
                                ?: 0),
                            vedtaksTypeKode = null,
                            vedtaksTypeNavn = null,
                            utbetaling = right?.verdi?.filter {
                                it.periode.tom.isBefore(LocalDate.now(clock)) || it.periode.tom.isEqual(
                                    LocalDate.now(clock)
                                )
                            }?.segmenter()?.map { utbetaling ->
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
                                    barnetilegg = utbetaling.verdi.gradertBarnetillegg()
                                        .toInt(),
                                    barnetillegg = utbetaling.verdi.gradertBarnetillegg()
                                        .toInt()
                                )
                            }.orEmpty(),
                            kildesystem = Kilde.valueOf(left.verdi.kildesystem),
                            samordningsId = left.verdi.samordningsId,
                            opphorsAarsak = left.verdi.opphorsAarsak
                        )
                    )
                }
            ).komprimer().segmenter().map { it.verdi }
                .filter { it.status == Status.LØPENDE.toString() || it.status == Status.AVSLUTTET.toString() }


        }

        return Maksimum(vedtak)
    }


    fun hentMediumFraKelvin(
        fnr: String,
        periode: Periode
    ): Medium {
        val kelvinData = behandlingsRepository.hentVedtaksData(fnr, periode)
        val vedtak: List<VedtakUtenUtbetaling> = kelvinData.flatMap { behandling ->
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

            rettighetsTypeTidslinje.kombiner(
                tilkjent,
                JoinStyle.LEFT_JOIN { periode, left, right ->
                    Segment(
                        periode,
                        VedtakUtenUtbetalingUtenPeriode(
                            vedtakId = behandling.vedtakId.toString(),
                            dagsats = right?.verdi?.dagsats ?: 0,
                            dagsatsEtterUføreReduksjon = right?.verdi?.regnUtDagsatsEtterUføreReduksjon()
                                ?: 0,
                            status = utledVedtakStatus(
                                behandling.behandlingStatus,
                                behandling.sak.status,
                                periode
                            ),
                            saksnummer = behandling.sak.saksnummer,
                            vedtaksdato = behandling.vedtaksDato,
                            rettighetsType = left.verdi,
                            beregningsgrunnlag = behandling.beregningsgrunnlag.toInt(),
                            barnMedStonad = right?.verdi?.antallBarn ?: 0,
                            kildesystem = Kilde.KELVIN.toString(),
                            samordningsId = behandling.samId,
                            opphorsAarsak = null,
                            barnetilleggSats = right?.verdi?.gradertBarnetillegg(),
                        )
                    )
                }
            ).komprimer()
                .segmenter()
                .map {
                    it.verdi.tilVedtakUtenUtbetaling(
                        no.nav.aap.api.intern.Periode(
                            it.periode.fom,
                            it.periode.tom
                        )
                    )
                }
                .filter { (it.status == Status.LØPENDE.toString() || it.status == Status.AVSLUTTET.toString()) }
        }

        return Medium(vedtak)
    }
}