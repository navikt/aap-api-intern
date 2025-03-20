package api.postgres

import api.maksimum.*
import no.nav.aap.api.intern.Kilde
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DatadelingDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.SakDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.TilkjentDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.UnderveisDTO
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek

class BehandlingsRepository(private val connection: DBConnection) {
    fun lagreBehandling(behandling: DatadelingDTO) {
        val gammelSak = connection.queryFirstOrNull(
            """SELECT ID FROM SAK WHERE SAKSNUMMER = ?""".trimIndent()
        ) {
            setParams {
                setString(1, behandling.sak.saksnummer)
            }
            setRowMapper { row ->
                row.getLong("ID")
            }
        }

        val sakId = gammelSak ?: connection.executeReturnKey(
            """
                INSERT INTO SAK (STATUS, RETTIGHETSPERIODE, SAKSNUMMER)
                VALUES (?, ?::daterange, ?)
            """.trimIndent()
        ) {
            setParams {
                setString(1, behandling.sak.status.toString())
                setPeriode(2, Periode(behandling.rettighetsPeriodeFom, behandling.rettighetsPeriodeTom))
                setString(3, behandling.sak.saksnummer)
            }
        }

        connection.executeBatch(
            """
                INSERT INTO SAK_PERSON (SAK_ID, PERSON_IDENT)
                VALUES (?, ?)
            """.trimIndent(),
            behandling.sak.fnr
        ) {
            setParams { fnr ->
                setLong(1, sakId)
                setString(2, fnr)
            }
        }

        val behandlingId = connection.queryFirstOrNull(
            """SELECT ID FROM BEHANDLING WHERE ID = ?""".trimIndent()
        ) {
            setParams {
                setLong(1, behandling.behandlingsId.toLong())
            }
            setRowMapper {
                it.getLong("ID")
            }
        }


        val nyBehandlingId = if (behandlingId != null) {
            connection.executeReturnKey(
                """
                UPDATE BEHANDLING SET STATUS = ?, vedtaks_dato = ?, OPPRETTET_TID = ?
                WHERE ID = ?
            """.trimIndent()
            ) {
                setParams {
                    setString(1, behandling.behandlingStatus.toString())
                    setLocalDate(2, behandling.vedtaksDato)
                    setLocalDateTime(3, behandling.sak.opprettetTidspunkt)
                    setLong(4, behandling.behandlingsId.toLong())
                }
            }
        } else {
            connection.executeReturnKey(
                """
                INSERT INTO BEHANDLING (SAK_ID, STATUS, VEDTAKS_DATO, TYPE, OPPRETTET_TID)
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent()
            ) {
                setParams {
                    setLong(1, sakId)
                    setString(2, behandling.behandlingStatus.toString())
                    setLocalDate(3, behandling.vedtaksDato)
                    setString(4, "TYPE")
                    setLocalDateTime(5, behandling.sak.opprettetTidspunkt)
                }
            }
        }



        connection.executeBatch(
            """
                INSERT INTO UNDERVEIS (BEHANDLING_ID, PERIODE, MELDEPERIODE, UTFALL, RETTIGHETS_TYPE, OPPRETTET_TID)
                VALUES (?, ?::daterange, ?::daterange, ?, ?, ?)
            """.trimIndent(),
            behandling.underveisperiode
        ) {
            setParams {
                setLong(1, nyBehandlingId)
                setPeriode(2, Periode(it.underveisFom, it.underveisTom))
                setPeriode(3, Periode(it.meldeperiodeFom, it.meldeperiodeTom))
                setString(4, it.utfall)
                setString(5, it.rettighetsType ?: "")
                setLocalDateTime(6, LocalDateTime.now())
            }
        }


        connection.execute(
            """DELETE FROM TILKJENT_YTELSE WHERE BEHANDLING_ID = ?""".trimIndent()
        ) {
            setParams {
                setLong(1, nyBehandlingId)
            }
        }
        val nytilkjentId = connection.executeReturnKey(
            """
                INSERT INTO TILKJENT_YTELSE (BEHANDLING_ID)
                VALUES (?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, nyBehandlingId)
            }
        }

        connection.executeBatch(
            """
                INSERT INTO TILKJENT_PERIODE (TILKJENT_YTELSE_ID, PERIODE, DAGSATS, GRADERING, GRUNNLAG, GRUNNLAGSFAKTOR, GRUNNBELOP, ANTALL_BARN, BARNETILLEGGSATS, BARNETILLEGG)
                VALUES (?, ?::daterange, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            behandling.tilkjent
        ) {
            setParams {
                setLong(1, nytilkjentId)
                setPeriode(2, Periode(it.tilkjentFom, it.tilkjentTom))
                setBigDecimal(3, it.dagsats.toBigDecimal())
                setInt(4, it.gradering)
                setBigDecimal(5, it.grunnlag)
                setBigDecimal(6, it.grunnlagsfaktor)
                setBigDecimal(7, it.grunnbeløp)
                setInt(8, it.antallBarn)
                setBigDecimal(9, it.barnetilleggsats)
                setBigDecimal(10, it.barnetillegg)
            }
        }
    }

    fun hentMedium(fnr: String, interval: Periode): Medium {
        val kelvinData = hentVedtaksData(fnr)
        val vedtak: List<VedtakUtenUtbetaling> = kelvinData.flatMap { behandling ->
            val underveisTidslinje = Tidslinje(
                behandling.underveisperiode.map {
                    Segment(
                        Periode(it.underveisFom, it.underveisTom),
                        UnderveisDB(
                            it.rettighetsType,
                            it.avslagsårsak
                        )
                    )
                }

            ).komprimer()

            val tilkjent = Tidslinje(
                behandling.tilkjent.map {
                    Segment(
                        Periode(it.tilkjentFom, it.tilkjentTom),
                        TilkjentDB(
                            it.dagsats,
                            it.grunnlag,
                            it.grunnlagsfaktor,
                            it.grunnbeløp,
                            it.antallBarn,
                            it.barnetilleggsats,
                            it.barnetillegg
                        )
                    )
                }
            )

            underveisTidslinje.kombiner(
                tilkjent,
                JoinStyle.INNER_JOIN { periode, left, right ->
                    Segment(
                        periode,
                        VedtakUtenUtbetalingUtenPeriode(
                            vedtaksId = behandling.behandlingsId,
                            dagsats = right.verdi.dagsats,
                            status =
                                if (behandling.behandlingStatus == no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.IVERKSETTES || periode.tom.isAfter(
                                        LocalDate.now()
                                    )
                                ) {
                                    Status.LØPENDE.toString()
                                } else if (behandling.behandlingStatus == no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.AVSLUTTET) {
                                    Status.AVSLUTTET.toString()
                                } else {
                                    Status.UTREDES.toString()
                                },
                            saksnummer = behandling.sak.saksnummer,
                            vedtaksdato = behandling.vedtaksDato.format(DateTimeFormatter.ISO_LOCAL_DATE),
                            vedtaksTypeKode = "",
                            vedtaksTypeNavn = "",
                            rettighetsType = left.verdi.rettighetsType ?: "",
                            beregningsgrunnlag = right.verdi.grunnlag.toInt(),
                            barnMedStonad = right.verdi.antallBarn,
                            kildesystem = Kilde.KELVIN.toString(),
                            samordningsId = null,
                            opphorsAarsak = null
                        )
                    )
                }
            ).komprimer()
                .map { it.verdi.tilVedtakUtenUtbetaling(no.nav.aap.api.intern.Periode(it.periode.fom, it.periode.tom)) }
                .filter { it.status == Status.LØPENDE.toString() || it.status == Status.AVSLUTTET.toString() }
        }

        return Medium(vedtak)
    }

    fun hentMaksimum(fnr: String, interval: Periode): Maksimum {
        val kelvinData = hentVedtaksData(fnr)
        val vedtak = kelvinData.map { behandling ->
            val underveisTidslinje = Tidslinje(
                behandling.underveisperiode.map {
                    Segment(
                        Periode(it.underveisFom, it.underveisTom),
                        UnderveisDB(
                            it.rettighetsType,
                            it.avslagsårsak
                        )
                    )
                }

            ).komprimer()

            val tilkjent = Tidslinje(
                behandling.tilkjent.map {
                    Segment(
                        Periode(it.tilkjentFom, it.tilkjentTom),
                        TilkjentDB(
                            it.dagsats,
                            it.grunnlag,
                            it.grunnlagsfaktor,
                            it.grunnbeløp,
                            it.antallBarn,
                            it.barnetilleggsats,
                            it.barnetillegg
                        )
                    )
                }
            )

            val tilkjentXUnderveis = underveisTidslinje.kombiner(
                tilkjent,
                JoinStyle.INNER_JOIN { periode, left, right ->
                    Segment(
                        periode, VedtakUtenUtbetaling(
                            vedtaksId = "",
                            dagsats = right.verdi.dagsats,
                            status = behandling.sak.status.toString(),
                            saksnummer = behandling.sak.saksnummer,
                            vedtaksdato = "",
                            vedtaksTypeKode = "",
                            vedtaksTypeNavn = "",
                            periode = no.nav.aap.api.intern.Periode(periode.fom, periode.tom),
                            rettighetsType = left.verdi.rettighetsType ?: "",
                            beregningsgrunnlag = right.verdi.grunnlag.toInt(),
                            barnMedStonad = right.verdi.antallBarn,
                            kildesystem = Kilde.KELVIN.toString(),
                            samordningsId = null,
                            opphorsAarsak = null
                        )
                    )
                }
            ).komprimer()
        }
        return Maksimum(
            vedtak = kelvinData.flatMap { behandling ->
                // filtrer ut tilkjentYtelsePerioder som ikke er avsluttet eller ikke har noen utbetaling
                val tilkjentYtelse =
                    behandling.tilkjent.filter { it.tilkjentTom <= LocalDate.now() && (it.dagsats.toInt() != 0 || it.gradering != 0) }


                val utbetalingPr2Uker = tilkjentYtelse.map { verdi ->
                    UtbetalingMedMer(
                        reduksjon = null,
                        utbetalingsgrad = verdi.gradering,
                        periode = no.nav.aap.api.intern.Periode(verdi.tilkjentFom, verdi.tilkjentTom),
                        belop = verdi.dagsats.toInt() * weekdaysBetween(
                            verdi.tilkjentFom,
                            verdi.tilkjentTom
                        ).toInt() * verdi.gradering / 100,
                        dagsats = verdi.dagsats.toInt(),
                        barnetilegg = verdi.barnetillegg.toInt(),
                    )
                }

                tilkjentYtelse.map { tilkjent ->
                    Vedtak(
                        vedtaksId = "",
                        dagsats = tilkjent.grunnlag.toInt(),
                        status = behandling.sak.status.toString(),
                        saksnummer = behandling.sak.saksnummer,
                        vedtaksdato = behandling.vedtaksDato.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        periode = no.nav.aap.api.intern.Periode(tilkjent.tilkjentFom, tilkjent.tilkjentTom),
                        rettighetsType = "",
                        beregningsgrunnlag = tilkjent.grunnlag.toInt() * 260,
                        barnMedStonad = tilkjent.antallBarn,
                        kildesystem = Kilde.KELVIN,
                        samordningsId = null,
                        opphorsAarsak = null,
                        vedtaksTypeKode = "",
                        vedtaksTypeNavn = "",
                        utbetaling = utbetalingPr2Uker.filter { it.periode.fraOgMedDato!! > tilkjent.tilkjentFom && it.periode.tilOgMedDato!! < tilkjent.tilkjentTom }
                    )
                }.filter { it.periode.fraOgMedDato!! <= interval.tom && it.periode.tilOgMedDato!! >= interval.fom }
            }
        )
    }

    fun hentVedtaksData(fnr: String): List<DatadelingDTO> {
        val sakerIder = connection.queryList<Long>(
            """
                SELECT SAK_ID FROM SAK_PERSON
                WHERE PERSON_IDENT = ?
            """.trimIndent()
        ) {
            setParams {
                setString(1, fnr)
            }
            setRowMapper { row ->
                row.getLong("SAK_ID")
            }
        }

        val saker = sakerIder.mapNotNull {
            connection.queryFirstOrNull<sakDB>(
                """
                    SELECT * FROM SAK
                    WHERE ID = ?
                """.trimIndent()
            ) {
                setParams {
                    setLong(1, it)
                }
                setRowMapper { row ->
                    sakDB(
                        saksnummer = row.getString("SAKSNUMMER"),
                        status = Status.valueOf(row.getString("STATUS")),
                        rettighetsPeriode = row.getPeriode("RETTIGHETSPERIODE"),
                        id = it
                    )
                }
            }
        }

        return saker.flatMap { sak ->
            val behandlinger = hentBehandlinger(sak.id)
            behandlinger.map { behandling ->

                DatadelingDTO(
                    behandlingsId = behandling.id.toString(),
                    underveisperiode = hentUnderveis(behandling.id),
                    rettighetsPeriodeFom = sak.rettighetsPeriode.fom,
                    rettighetsPeriodeTom = sak.rettighetsPeriode.tom,
                    behandlingStatus = behandling.behandlingStatus,
                    vedtaksDato = behandling.vedtaksDato,
                    sak = SakDTO(
                        saksnummer = sak.saksnummer,
                        status = sak.status,
                        fnr = emptyList()
                    ),
                    tilkjent = hentTilkjentYtelse(behandling.id)
                )
            }
        }


    }

    fun hentBehandlinger(sakId: Long): List<BehandlingDB> {
        return connection.queryList(
            """
                SELECT * FROM BEHANDLING
                WHERE SAK_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, sakId)
            }
            setRowMapper { row ->
                BehandlingDB(
                    id = row.getLong("ID"),
                    behandlingStatus = no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.valueOf(row.getString("STATUS")),
                    vedtaksDato = row.getLocalDate("VEDTAKS_DATO"),
                    type = row.getString("TYPE"),
                    oppretterTidspunkt = row.getLocalDate("OPPRETTET_TID")
                )
            }
        }
    }

    fun hentUnderveis(behandlingId: Long): List<UnderveisDTO> {
        return connection.queryList(
            """
                SELECT * FROM UNDERVEIS
                WHERE BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId)
            }
            setRowMapper { row ->
                val periode = row.getPeriode("PERIODE")
                val meldePeriode = row.getPeriode("MELDEPERIODE")
                UnderveisDTO(
                    underveisFom = periode.fom,
                    underveisTom = periode.tom,
                    meldeperiodeFom = meldePeriode.fom,
                    meldeperiodeTom = meldePeriode.tom,
                    utfall = row.getString("UTFALL"),
                    rettighetsType = row.getString("RETTIGHETS_TYPE"),
                    avslagsårsak = null
                )
            }
        }
    }

    fun hentTilkjentYtelse(behandlingId: Long): List<TilkjentDTO> {
        return connection.queryList(
            """
                SELECT * FROM TILKJENT_PERIODE
                WHERE TILKJENT_YTELSE_ID IN (
                    SELECT ID FROM TILKJENT_YTELSE
                    WHERE BEHANDLING_ID = ?
                )
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId) }
            setRowMapper {
                TilkjentDTO(
                    tilkjentFom = it.getPeriode("PERIODE").fom,
                    tilkjentTom = it.getPeriode("PERIODE").tom,
                    dagsats = it.getBigDecimal("DAGSATS").toInt(),
                    gradering = it.getInt("GRADERING"),
                    grunnlag = it.getBigDecimal("GRUNNLAG"),
                    grunnlagsfaktor = it.getBigDecimal("GRUNNLAGSFAKTOR"),
                    grunnbeløp = it.getBigDecimal("GRUNNBELOP"),
                    antallBarn = it.getInt("ANTALL_BARN"),
                    barnetilleggsats = it.getBigDecimal("BARNETILLEGGSATS"),
                    barnetillegg = it.getBigDecimal("BARNETILLEGG")
                )
            }
        }
    }
}

data class sakDB(
    val id: Long,
    val status: Status,
    val rettighetsPeriode: Periode,
    val saksnummer: String,
)

data class BehandlingDB(
    val id: Long,
    val behandlingStatus: no.nav.aap.behandlingsflyt.kontrakt.behandling.Status,
    val vedtaksDato: LocalDate,
    val type: String,
    val oppretterTidspunkt: LocalDate,
)

data class UnderveisDB(
    val rettighetsType: String?,
    val avslagsårsak: String?,
)

data class TilkjentDB(
    val dagsats: Int,
    val grunnlag: BigDecimal,
    val grunnlagsfaktor: BigDecimal,
    val grunnbeløp: BigDecimal,
    val antallBarn: Int,
    val barnetilleggsats: BigDecimal,
    val barnetillegg: BigDecimal,
)

data class UnderveisXTilkjent(
    val rettighetsType: String?,
    val avslagsårsak: String?,
    val dagsats: Int,
    val gradering: Int,
    val grunnlag: BigDecimal,
    val grunnlagsfaktor: BigDecimal,
    val grunnbeløp: BigDecimal,
    val antallBarn: Int,
    val barnetilleggsats: BigDecimal,
    val barnetillegg: BigDecimal,
)

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

fun mergeTilkjentPeriods(periods: List<TilkjentDTO>): List<TilkjentDTO> {
    if (periods.isEmpty()) return emptyList()

    val sortedPeriods = periods.sortedBy { it.tilkjentFom }.filter { it.dagsats.toInt() != 0 }
    val mergedPeriods = mutableListOf<TilkjentDTO>()

    var currentPeriod = sortedPeriods[0]

    for (i in 1 until sortedPeriods.size) {
        val nextPeriod = sortedPeriods[i]

        if (currentPeriod.dagsats == nextPeriod.dagsats &&
            currentPeriod.grunnlag == nextPeriod.grunnlag &&
            currentPeriod.grunnlagsfaktor == nextPeriod.grunnlagsfaktor &&
            currentPeriod.grunnbeløp == nextPeriod.grunnbeløp &&
            currentPeriod.antallBarn == nextPeriod.antallBarn &&
            currentPeriod.barnetilleggsats == nextPeriod.barnetilleggsats &&
            currentPeriod.barnetillegg == nextPeriod.barnetillegg &&
            currentPeriod.tilkjentTom.plusDays(1) == nextPeriod.tilkjentFom
        ) {
            // Merge the periods
            currentPeriod = currentPeriod.copy(tilkjentTom = nextPeriod.tilkjentTom)
        } else {
            // Add the current period to the list and start a new one
            mergedPeriods.add(currentPeriod)
            currentPeriod = nextPeriod
        }
    }

    // Add the last period
    mergedPeriods.add(currentPeriod)

    return mergedPeriods
}