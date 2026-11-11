package no.nav.aap.api.kelvin

import com.papsign.ktor.openapigen.annotations.properties.description.Description
import no.nav.aap.api.maksimum.InternKilde
import no.nav.aap.api.maksimum.InternMaksimum
import no.nav.aap.api.maksimum.InternMedium
import no.nav.aap.api.maksimum.InternPeriode
import no.nav.aap.api.maksimum.InternUtbetalingMedMer
import no.nav.aap.api.maksimum.InternVedtak
import no.nav.aap.api.maksimum.InternVedtakUtenUtbetaling
import no.nav.aap.api.postgres.BehandlingsRepository
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import java.math.BigDecimal
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate

private const val LØPENDE = "LØPENDE"

class VedtakService(
    private val behandlingsRepository: BehandlingsRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) {

fun hentMaksimum(fnr: String, interval: Periode): InternMaksimum {
        val kelvinData = behandlingsRepository.hentVedtaksData(fnr, interval)
        val vedtak = kelvinData.flatMap { behandling ->
            val perioderTidslinje = behandling.rettighetsTypeTidslinje.kombiner(
                behandling.tilkjent,
                JoinStyle.LEFT_JOIN { periode, (_, rettighetsType), right ->
                    val tilkjentYtelse = right?.verdi

                    Segment(
                        periode,
                        VedtakUtenUtbetalingUtenPeriode(
                            vedtakId = behandling.vedtakId.toString(),
                            dagsats = tilkjentYtelse?.dagsats.ellerNull(),
                            dagsatsEtterUføreReduksjon = tilkjentYtelse?.regnUtDagsatsEtterUføreReduksjon()
                                .ellerNull(),
                            status = LØPENDE,
                            saksnummer = behandling.sak.saksnummer,
                            vedtaksdato = behandling.vedtaksDato,
                            rettighetsType = rettighetsType,
                            beregningsgrunnlag = behandling.beregningsgrunnlag?.toInt().ellerNull(),
                            barnMedStonad = tilkjentYtelse?.antallBarn.ellerNull(),
                            barnetilleggSats = tilkjentYtelse?.barnetilleggsats,
                            kildesystem = InternKilde.KELVIN,
                            samordningOgTpnr = behandling.samIdOgTpNr,
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
                behandling.tilkjentUtenHelger.splittOppIPerioder(
                    perioderMedArena.perioder().toList()
                )

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
                        InternVedtak(
                            vedtakId = vedtakUtenUtbetalingUtenPeriode.vedtakId,
                            dagsats = vedtakUtenUtbetalingUtenPeriode.dagsats,
                            dagsatsEtterUføreReduksjon = vedtakUtenUtbetalingUtenPeriode.dagsatsEtterUføreReduksjon,
                            status = vedtakUtenUtbetalingUtenPeriode.status,
                            saksnummer = vedtakUtenUtbetalingUtenPeriode.saksnummer,
                            vedtaksdato = vedtakUtenUtbetalingUtenPeriode.vedtaksdato,
                            periode = InternPeriode(periode.fom, periode.tom),
                            rettighetsType = vedtakUtenUtbetalingUtenPeriode.rettighetsType,
                            beregningsgrunnlag = vedtakUtenUtbetalingUtenPeriode.beregningsgrunnlag,
                            barnMedStonad = vedtakUtenUtbetalingUtenPeriode.barnMedStonad,
                            barnetillegg = vedtakUtenUtbetalingUtenPeriode.barnMedStonad * tilkjentYtelseTidslinje.segmenter()
                                .lastOrNull()?.verdi?.barnetilleggsats?.toInt().ellerNull(),
                            barnetilleggSats = vedtakUtenUtbetalingUtenPeriode.barnetilleggSats?.toInt()
                                .ellerNull(),
                            vedtaksTypeKode = vedtaksTypeKode,
                            vedtaksTypeNavn = null,
                            utbetaling = hentTilkjentYtelseForPeriode(
                                tilkjentYtelseTidslinje,
                                periode
                            ),
                            kildesystem = vedtakUtenUtbetalingUtenPeriode.kildesystem,
                            samIdOgTpnr = vedtakUtenUtbetalingUtenPeriode.samordningOgTpnr,
                            opphorsAarsak = vedtakUtenUtbetalingUtenPeriode.opphorsAarsak,
                        )
                    )
                    segment
                }
            ).komprimer().segmenter().map { it.verdi }
        }

        return InternMaksimum(vedtak)
    }

    private fun hentTilkjentYtelseForPeriode(
        tilkjentYtelseTidslinje: Tidslinje<TilkjentYtelse>,
        periode: Periode
    ): List<InternUtbetalingMedMer> = tilkjentYtelseTidslinje
        .takeUnless { LocalDate.now(clock) < periode.fom }
        ?.begrensetTil(Periode(periode.fom, LocalDate.now(clock)))
        .orEmpty()
        .komprimer()
        .segmenter()
        .map { (periode, verdi) ->
            val utbetaling = verdi

            val dagsats = utbetaling.dagsats
            val gradering = utbetaling.gradering

            val beløpPerDag = utbetaling.effektivDagsats
                ?: ((dagsats + utbetaling.barnetillegg.toInt()) * gradering / 100)

            InternUtbetalingMedMer(
                reduksjon = null,
                utbetalingsgrad = gradering,
                periode = InternPeriode(periode.fom, periode.tom),
                // TODO: bør hente korrekt beløp på samme måte som i behandlingsflyt
                belop = beløpPerDag * weekdaysBetween(
                    periode.fom,
                    periode.tom
                ),
                dagsats = dagsats * gradering / 100,
                barnetillegg = utbetaling.gradertBarnetillegg().toInt()
            )
        }


    fun hentMediumFraKelvin(
        fnr: String,
        periode: Periode
    ): InternMedium {
        val kelvinData = behandlingsRepository.hentVedtaksData(fnr, periode)
        val vedtak: List<InternVedtakUtenUtbetaling> = kelvinData.flatMap { behandling ->
            behandling.rettighetsTypeTidslinje.kombiner(
                behandling.tilkjent,
                JoinStyle.LEFT_JOIN { periode, left, right ->
                    val tilkjentYtelse = right?.verdi
                    Segment(
                        periode,
                        VedtakUtenUtbetalingUtenPeriode(
                            vedtakId = behandling.vedtakId.toString(),
                            dagsats = tilkjentYtelse?.dagsats.ellerNull(),
                            dagsatsEtterUføreReduksjon = tilkjentYtelse?.regnUtDagsatsEtterUføreReduksjon()
                                .ellerNull(),
                            status = LØPENDE,
                            saksnummer = behandling.sak.saksnummer,
                            vedtaksdato = behandling.vedtaksDato,
                            rettighetsType = left.verdi,
                            beregningsgrunnlag = behandling.beregningsgrunnlag?.toInt().ellerNull(),
                            barnMedStonad = tilkjentYtelse?.antallBarn.ellerNull(),
                            kildesystem = InternKilde.KELVIN,
                            samordningOgTpnr = behandling.samIdOgTpNr,
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
                        InternPeriode(
                            it.periode.fom,
                            it.periode.tom
                        ),
                        behandling.nyttVedtak,
                    )
                }
                .verdier()
        }

        return InternMedium(vedtak)
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
    @property:Description("ID som deles i forbindelse med samordning.")
    val vedtakId: String,
    @property:Description("Full dagsats før reduksjoner.")
    val dagsats: Int,
    @property:Description("Dagsats etter uføre-reduksjon. Dette er lik dagsats * (100 - uføregrad) / 100. Kommer kun fra nytt system (Kelvin). Ved manglende data er denne null.")
    val dagsatsEtterUføreReduksjon: Int,
    @property:Deprecated("Vil fjernes.")
    @property:Description("Status på en sak. Per i dag konstant lik LØPENDE.")
    val status: String,
    val saksnummer: String,
    val vedtaksdato: LocalDate, //reg_dato
    @property:Description("Rettighetsgruppe. For data fra Arena er dette aktivitetsfasekode.")
    val rettighetsType: String, ////aktivitetsfase //Aktfasekode
    val beregningsgrunnlag: Int,

    /** Antall barn som gir rett til barnetillegg.  */
    @property:Description("Antall barn som gir rett til barnetillegg.")
    val barnMedStonad: Int,

    @property:Description("Kildesystem for vedtak. Mulige verdier er ARENA og KELVIN.")
    val kildesystem: InternKilde,
    val samordningOgTpnr: List<SamIdOgTpnr> = emptyList(),
    val opphorsAarsak: String? = null,
    val lopenrvedtak: Int? = null,
    val relatertVedtak: Int? = null,

    /** Størrelsen på ugradert barnetilleggsats.
     *
     * Verdien er ugradert, i den forstand at:
     * Hvis barnetilleggsatsen er spesifisert i AAP-forskriften § 8 til 38 kroner, og medlemmet får 50% AAP,
     * så vil [barnetilleggSats] være 38.
     **/
    @property:Description(
        """
     Størrelsen på ugradert barnetilleggsats.
    
    Verdien er ugradert, i den forstand at:
    Hvis barnetilleggsatsen er spesifisert i AAP-forskriften § 8 til 38 kroner, og medlemmet får 50% AAP,
    så vil [barnetilleggSats] være 38.
    """
    )
    val barnetilleggSats: BigDecimal? = null,
    val arenavedtak: Arenavedtak? = null,
) {
    fun tilVedtakUtenUtbetaling(
        periode: InternPeriode,
        nyttVedtak: Boolean
    ): InternVedtakUtenUtbetaling {
        val vedtaksTypeKode = arenavedtak?.vedtaksvariant?.typeKode
            ?: if (nyttVedtak) "O" else "E"
        return InternVedtakUtenUtbetaling(
            dagsats = this.dagsats,
            dagsatsEtterUføreReduksjon = this.dagsatsEtterUføreReduksjon,
            vedtakId = this.vedtakId,
            status = this.status,
            saksnummer = this.saksnummer,
            vedtaksdato = this.vedtaksdato,
            vedtaksTypeKode = vedtaksTypeKode,
            vedtaksTypeNavn = null,
            periode = periode,
            rettighetsType = this.rettighetsType,
            beregningsgrunnlag = this.beregningsgrunnlag,
            barnMedStonad = this.barnMedStonad,
            barnetillegg = barnMedStonad * this.barnetilleggSats?.toInt().ellerNull(),
            kildesystem = this.kildesystem,
            samIdOgTpnr = this.samordningOgTpnr,
            opphorsAarsak = this.opphorsAarsak,
            lopenrvedtak = this.lopenrvedtak,
            relatertVedtak = this.relatertVedtak,
        )
    }
}

fun Int?.ellerNull(): Int = this ?: 0
