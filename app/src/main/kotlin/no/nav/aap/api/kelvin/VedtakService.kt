package no.nav.aap.api.kelvin

import com.papsign.ktor.openapigen.annotations.properties.description.Description
import no.nav.aap.api.intern.*
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.komponenter.tidslinje.*
import no.nav.aap.komponenter.type.Periode
import java.math.BigDecimal
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate

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
                            status = Status.LØPENDE.name, // TODO
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
                    val vedtakUtenUtbetalingUtenPeriode = left.verdi
                    val vedtaksTypeKode =
                        vedtakUtenUtbetalingUtenPeriode.arenavedtak?.vedtaksvariant?.typeKode
                            ?: if (behandling.nyttVedtak) "O" else "E"
                    val tilkjentYtelseTidslinje = right?.verdi.orEmpty()
                    val segment = Segment(
                        periode,
                        Vedtak(
                            vedtakId = vedtakUtenUtbetalingUtenPeriode.vedtakId,
                            dagsats = vedtakUtenUtbetalingUtenPeriode.dagsats,
                            dagsatsEtterUføreReduksjon = vedtakUtenUtbetalingUtenPeriode.dagsatsEtterUføreReduksjon,
                            status = vedtakUtenUtbetalingUtenPeriode.status,
                            saksnummer = vedtakUtenUtbetalingUtenPeriode.saksnummer,
                            vedtaksdato = vedtakUtenUtbetalingUtenPeriode.vedtaksdato,
                            periode = no.nav.aap.api.intern.Periode(periode.fom, periode.tom),
                            rettighetsType = vedtakUtenUtbetalingUtenPeriode.rettighetsType,
                            beregningsgrunnlag = vedtakUtenUtbetalingUtenPeriode.beregningsgrunnlag,
                            barnMedStonad = vedtakUtenUtbetalingUtenPeriode.barnMedStonad,
                            barnetillegg = vedtakUtenUtbetalingUtenPeriode.barnMedStonad * (tilkjentYtelseTidslinje.segmenter()
                                .firstOrNull()?.verdi?.barnetilleggsats?.toInt()
                                ?: 0),
                            barnetilleggSats = vedtakUtenUtbetalingUtenPeriode.barnetilleggSats?.toInt()
                                ?: 0,
                            vedtaksTypeKode = vedtaksTypeKode,
                            vedtaksTypeNavn = null,
                            utbetaling = hentTilkjentYtelseForPeriode(
                                tilkjentYtelseTidslinje,
                                periode
                            ),
                            kildesystem = vedtakUtenUtbetalingUtenPeriode.kildesystem,
                            samordningsId = vedtakUtenUtbetalingUtenPeriode.samordningsId,
                            opphorsAarsak = vedtakUtenUtbetalingUtenPeriode.opphorsAarsak
                        )
                    )
                    segment
                }
            ).komprimer().segmenter().map { it.verdi }
                .filter { it.status == Status.LØPENDE.toString() || it.status == Status.AVSLUTTET.toString() }
        }

        return Maksimum(vedtak)
    }

    fun tilkjentYtelseForPeriode(fnr: String, periode: Periode): List<Segment<TilkjentYtelse>> {
        return behandlingsRepository.hentVedtaksData(fnr, periode)
            // Antar høyeste vedtak-id er nyeste behandling.
            .maxByOrNull { it.vedtakId }
            ?.tilkjent.orEmpty()
            .begrensetTil(periode)
            .segmenter()
            .toList()
    }

    private fun hentTilkjentYtelseForPeriode(
        tilkjentYtelseTidslinje: Tidslinje<TilkjentYtelse>,
        periode: Periode
    ): List<UtbetalingMedMer> = tilkjentYtelseTidslinje
        .takeUnless { LocalDate.now(clock) < periode.fom }
        ?.begrensetTil(Periode(periode.fom, LocalDate.now(clock)))
        .orEmpty()
        .komprimer()
        .segmenter()
        .map { utbetaling ->
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
                    val tilkjentYtelse = right?.verdi
                    Segment(
                        periode,
                        VedtakUtenUtbetalingUtenPeriode(
                            vedtakId = behandling.vedtakId.toString(),
                            dagsats = tilkjentYtelse?.dagsats ?: 0,
                            dagsatsEtterUføreReduksjon = tilkjentYtelse?.regnUtDagsatsEtterUføreReduksjon()
                                ?: 0,
                            status = Status.LØPENDE.name, // TODO!
                            saksnummer = behandling.sak.saksnummer,
                            vedtaksdato = behandling.vedtaksDato,
                            rettighetsType = left.verdi,
                            beregningsgrunnlag = behandling.beregningsgrunnlag?.toInt() ?: 0,
                            barnMedStonad = tilkjentYtelse?.antallBarn ?: 0,
                            kildesystem = Kilde.KELVIN,
                            samordningsId = behandling.samId,
                            opphorsAarsak = null,
                            barnetilleggSats = tilkjentYtelse?.barnetilleggsats,
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
                .filter { (it.verdi.status == Status.LØPENDE.toString() || it.verdi.status == Status.AVSLUTTET.toString()) }
                .verdier()
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
    @param:Description("ID som deles i forbindelse med samordning.")
    val vedtakId: String,
    @param:Description("Full dagsats før reduksjoner.")
    val dagsats: Int,
    @param:Description("Dagsats etter uføre-reduksjon. Dette er lik dagsats * (100 - uføregrad) / 100. Kommer kun fra nytt system (Kelvin). Ved manglende data er denne null.")
    val dagsatsEtterUføreReduksjon: Int,
    @param:Description("Status på et vedtak. Mulige verdier er LØPENDE, AVSLUTTET, UTREDES. Per i dag konstant lik LØPENDE.")
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
    fun tilVedtakUtenUtbetaling(
        periode: no.nav.aap.api.intern.Periode,
        nyttVedtak: Boolean
    ): VedtakUtenUtbetaling {
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
