package no.nav.aap.api.kelvin

import com.papsign.ktor.openapigen.annotations.properties.description.Description
import java.math.BigDecimal
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import no.nav.aap.api.intern.Kilde
import no.nav.aap.api.intern.Maksimum
import no.nav.aap.api.intern.Medium
import no.nav.aap.api.intern.UtbetalingMedMer
import no.nav.aap.api.intern.Vedtak
import no.nav.aap.api.intern.VedtakUtenUtbetaling
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode

class VedtakService(
    private val behandlingsRepository: BehandlingsRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    fun hentMaksimum(fnr: String, interval: Periode): Maksimum {
        val kelvinData = behandlingsRepository.hentVedtaksData(fnr, interval)
        val vedtak = kelvinData.flatMap { behandling ->
            val perioderTidslinje = behandling.rettighetsTypeTidslinje.kombiner(
                behandling.tilkjent,
                JoinStyle.LEFT_JOIN { periode, left, right ->
                    Segment(
                        periode,
                        VedtakUtenUtbetalingUtenPeriode(
                            vedtakId = behandling.vedtakId.toString(),
                            dagsats = right?.verdi?.dagsats ?: 0,
                            dagsatsEtterUføreReduksjon = right?.verdi?.regnUtDagsatsEtterUføreReduksjon()
                                ?: 0,
                            status = Status.LØPENDE.toString(), // TODO
                            saksnummer = behandling.sak.saksnummer,
                            vedtaksdato = behandling.vedtaksDato,
                            rettighetsType = left.verdi,
                            beregningsgrunnlag = behandling.beregningsgrunnlag?.toInt() ?: 0,
                            barnMedStonad = right?.verdi?.antallBarn ?: 0,
                            barnetilleggSats = right?.verdi?.barnetilleggsats,
                            kildesystem = Kilde.KELVIN,
                            samordningsId = behandling.samId,
                            opphorsAarsak = null,
                        )
                    )
                }
            ).komprimer()
            val perioderMedArena =
                perioderTidslinje.leftJoin(behandling.arenakompatibleVedtakTidslinje) { left, right ->
                    left.copy(arenavedtak = right)
                }

            val tilkjentPerioder =
                behandling.tilkjent.splittOppIPerioder(perioderMedArena.perioder().toList())

            perioderMedArena.kombiner(
                tilkjentPerioder,
                JoinStyle.LEFT_JOIN { periode, left, right ->
                    val vedtaksTypeKode = left.verdi.arenavedtak?.vedtaksvariant?.typeKode
                        ?: if (behandling.nyttVedtak) "O" else "E"
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
                            barnetilleggSats = left.verdi.barnetilleggSats?.toInt() ?: 0,
                            vedtaksTypeKode = vedtaksTypeKode,
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
                                    barnetillegg = utbetaling.verdi.gradertBarnetillegg()
                                        .toInt()
                                )
                            }.orEmpty(),
                            kildesystem = left.verdi.kildesystem,
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
            behandling.rettighetsTypeTidslinje.kombiner(
                behandling.tilkjent,
                JoinStyle.LEFT_JOIN { periode, left, right ->
                    Segment(
                        periode,
                        VedtakUtenUtbetalingUtenPeriode(
                            vedtakId = behandling.vedtakId.toString(),
                            dagsats = right?.verdi?.dagsats ?: 0,
                            dagsatsEtterUføreReduksjon = right?.verdi?.regnUtDagsatsEtterUføreReduksjon()
                                ?: 0,
                            status = Status.LØPENDE.toString(), // TODO!
                            saksnummer = behandling.sak.saksnummer,
                            vedtaksdato = behandling.vedtaksDato,
                            rettighetsType = left.verdi,
                            beregningsgrunnlag = behandling.beregningsgrunnlag?.toInt() ?: 0,
                            barnMedStonad = right?.verdi?.antallBarn ?: 0,
                            kildesystem = Kilde.KELVIN,
                            samordningsId = behandling.samId,
                            opphorsAarsak = null,
                            barnetilleggSats = right?.verdi?.barnetilleggsats,
                        )
                    )
                }
            ).komprimer()
                .let { perioderTidslinje ->
                    perioderTidslinje.leftJoin(behandling.arenakompatibleVedtakTidslinje) { periode, left, right ->
                        Segment(periode, left.copy(arenavedtak = right))
                    }

                }
                .map {
                    it.verdi.tilVedtakUtenUtbetaling(
                        no.nav.aap.api.intern.Periode(
                            it.periode.fom,
                            it.periode.tom
                        ),
                        behandling.nyttVedtak,
                    )
                }
                .filter { (it.verdi.status == Status.LØPENDE.toString() || it.verdi.status == Status.AVSLUTTET.toString()) }.verdier()
        }

        return Medium(vedtak)
    }
}

fun weekdaysBetween(startDate: LocalDate, endDate: LocalDate): Int {
    var count = 0
    var date = startDate

    while (!date.isAfter(endDate)) {
        if (date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY) {
            count++
        }
        date = date.plusDays(1)
    }

    return count
}

/**
 * @param vedtakId Svarer til ID til vedtak-tabellen i behandlingsflyt.
 */
data class VedtakUtenUtbetalingUtenPeriode(
    val vedtakId: String,
    @param:Description("Full dagsats før reduksjoner.")
    val dagsats: Int,
    @param:Description("Dagsats etter uføre-reduksjon. Dette er lik dagsats * (100 - uføregrad) / 100. Kommer kun fra nytt system (Kelvin). Ved manglende data er denne null.")
    val dagsatsEtterUføreReduksjon: Int,
    @param:Description("Status på et vedtak. Mulige verdier er LØPENDE, AVSLUTTET, UTREDES.")
    val status: String,
    val saksnummer: String,
    val vedtaksdato: LocalDate, //reg_dato
    @param:Description("Rettighetsgruppe. For data fra Arena er dette aktivitetsfasekode.")
    val rettighetsType: String, ////aktivitetsfase //Aktfasekode
    val beregningsgrunnlag: Int,

    @param:Description("Antall barn som gir rett til barnetillegg.")
    /** Antall barn som gir rett til barnetillegg.  */
    val barnMedStonad: Int,

    @param:Description("Kildesystem for vedtak. Mulige verdier er ARENA og KELVIN.")
    val kildesystem: Kilde,
    val samordningsId: String? = null,
    val opphorsAarsak: String? = null,

    @param:Description(
        """
     Størrelsen på ugradert barnetilleggsats.
    
    Verdien er ugradert, i den forstand at:
    Hvis barnetilleggsatsen er spesifisert i AAP-forskriften § 8 til 38 kroner, og medlemmet får 50% AAP,
    så vil [barnetilleggSats] være 38.
    """
    )
    /** Størrelsen på ugradert barnetilleggsats.
     *
     * Verdien er ugradert, i den forstand at:
     * Hvis barnetilleggsatsen er spesifisert i AAP-forskriften § 8 til 38 kroner, og medlemmet får 50% AAP,
     * så vil [barnetilleggSats] være 38.
     **/
    val barnetilleggSats: BigDecimal? = null,
    val arenavedtak: Arenavedtak? = null,
) {
    fun tilVedtakUtenUtbetaling(periode: no.nav.aap.api.intern.Periode, nyttVedtak: Boolean): VedtakUtenUtbetaling {
        val vedtaksTypeKode = arenavedtak?.vedtaksvariant?.typeKode
            ?: if (nyttVedtak) "O" else "E"
        return VedtakUtenUtbetaling(
            vedtakId = this.vedtakId,
            dagsats = this.dagsats,
            dagsatsEtterUføreReduksjon = this.dagsatsEtterUføreReduksjon,
            status = this.status,
            saksnummer = this.saksnummer,
            vedtaksdato = this.vedtaksdato,
            vedtaksTypeKode = vedtaksTypeKode,
            vedtaksTypeNavn = null,
            periode = periode,
            rettighetsType = this.rettighetsType,
            beregningsgrunnlag = this.beregningsgrunnlag,
            barnMedStonad = this.barnMedStonad,
            barnetillegg = barnMedStonad * (this.barnetilleggSats?.toInt() ?: 0),
            kildesystem = this.kildesystem,
            samordningsId = this.samordningsId,
            opphorsAarsak = this.opphorsAarsak
        )
    }
}
