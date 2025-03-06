package api.postgres

import api.maksimum.Maksimum
import api.maksimum.UtbetalingMedMer
import api.maksimum.Vedtak
import api.vedtak.Utbetaling
import no.nav.aap.api.intern.Status
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DatadelingDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.SakDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.TilkjentDTO
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.UnderveisDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetsType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BehandlingsRepository(private val connection: DBConnection) {
    fun lagreBehandling(fnr: String, behandling: DatadelingDTO) {
        val sakId = connection.executeReturnKey(
            """
                INSERT INTO SAK (STATUS, RETTIGHETS_PERIODE, SAKSNUMMER)
                VALUES (?, ?::daterange, ?, ?)
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
            setParams {
                setLong(1, sakId)
                setString(2, it)
            }
        }

        val behandlingId = connection.executeReturnKey(
            """
                INSERT INTO BEHANDLING (SAK_ID, STATUS, TYPE, VEDTAKS_DATO, OPPRETTET_TID)
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, sakId)
                setString(2, behandling.behandlingStatus.toString())
                setString(3, "TYPE")
                setLocalDate(4, behandling.vedtaksDato)
                setLocalDateTime(5, behandling.sak.opprettetTidspunkt)
            }
        }

        connection.executeBatch(
            """
                INSERT INTO UNDERVEIS (BEHANDLING_ID, PERIODE, MELDEPERIODE, UTFALL, RETTIGHETS_TYPE, OPPRETTET_TID)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            behandling.underveisperiode
        ) {
            setParams {
                setLong(1, behandlingId)
                setPeriode(2, Periode(it.underveisFom, it.underveisTom))
                setPeriode(3, Periode(it.meldeperiodeFom, it.meldeperiodeTom))
                setString(6, it.utfall)
                setString(7, it.rettighetsType?.toString() ?: "")
                setString(8, it.avslagsårsak)
            }
        }

        val tilkjentId = connection.executeReturnKey(
            """
                INSERT INTO TILKJENT_YTELSE (BEHANDLING_ID)
                VALUES (?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId)
            }
        }

        connection.executeBatch(
            """
                INSERT INTO TILKJENT_PERIODE (TILKJENT_YTELSE_ID, TILKJENT_FOM, TILKJENT_TOM, DAGSATS, GRADERING, GRUNNLAG, GRUNNLAGSFAKTOR, GRUNNBELØP, ANTALL_BARN, BARNETILLEGGSATS, BARNETILLEGG)
            """.trimIndent(),
            behandling.tilkjent
        ) {
            setParams {
                setLong(1, tilkjentId)
                setLocalDate(2, it.tilkjentFom)
                setLocalDate(3, it.tilkjentTom)
                setInt(4, it.dagsats)
                setInt(5, it.gradering)
                setInt(6, it.grunnlag)
                setInt(7, it.grunnlagsfaktor)
                setInt(8, it.grunnbeløp)
                setInt(9, it.antallBarn)
                setInt(10, it.barnetilleggsats)
                setInt(11, it.barnetillegg)
            }
        }
    }

    fun hentMaksimumArenaOppsett(fnr: String): Maksimum {
        val kelvinData = hentVedtaksData(fnr)

        return Maksimum(
            vedtak = kelvinData.flatMap { sak ->
                // filtrer ut tilkjentYtelsePerioder som ikke er avsluttet eller ikke har noen utbetaling
                val tilkjentYtelse =
                    sak.tilkjent.filter { it.tilkjentTom <= LocalDate.now() && it.dagsats != 0 && it.gradering > 0 }


                val utbetalingPr2Uker = tilkjentYtelse.map { verdi ->
                    UtbetalingMedMer(
                        reduksjon = null,
                        utbetalingsgrad = verdi.gradering,
                        periode = no.nav.aap.api.intern.Periode(verdi.tilkjentFom, verdi.tilkjentTom),
                        belop = verdi.dagsats * 10 * verdi.gradering / 100,
                        dagsats = verdi.dagsats,
                        barnetilegg = verdi.barnetillegg,
                    )
                }

                var prev: TilkjentDTO? = null
                val vedtakTilkjentYtelse = mergeTilkjentPeriods(tilkjentYtelse)
                vedtakTilkjentYtelse.map { tilkjent ->
                    Vedtak(
                        vedtaksId = "",
                        dagsats = tilkjent.dagsats,
                        status = sak.behandlingStatus.toString(),
                        saksnummer = sak.sak.saksnummer,
                        vedtaksdato = sak.vedtaksDato.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        periode = no.nav.aap.api.intern.Periode(tilkjent.tilkjentFom, tilkjent.tilkjentTom),
                        rettighetsType = "",
                        beregningsgrunnlag = tilkjent.grunnlag,
                        barnMedStonad = tilkjent.antallBarn,
                        kildesystem = "KELVIN",
                        samordningsId = null,
                        opphorsAarsak = null,
                        vedtaksTypeKode = "",
                        vedtaksTypeNavn = "",
                        utbetaling = utbetalingPr2Uker.filter { it.periode.fraOgMedDato!! > tilkjent.tilkjentFom && it.periode.tilOgMedDato!! < tilkjent.tilkjentTom }
                    )
                }
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
                        status = no.nav.aap.behandlingsflyt.kontrakt.sak.Status.valueOf(row.getString("STATUS")),
                        opprettetTidspunkt = row.getLocalDateTime("OPPRETTET_TID"),
                        rettighetsPeriode = row.getPeriode("RETTIGHETS_PERIODE"),
                        id = it
                    )
                }
            }
        }

        return saker.mapNotNull { sak ->
            val behandling = hentBehandling(sak.id)
            DatadelingDTO(
                underveisperiode = hentUnderveis(behandling!!.id),
                rettighetsPeriodeFom = sak.rettighetsPeriode.fom,
                rettighetsPeriodeTom = sak.rettighetsPeriode.tom,
                behandlingStatus = behandling.behandlingStatus,
                vedtaksDato = behandling.vedtaksDato,
                sak = SakDTO(
                    saksnummer = sak.saksnummer,
                    status = sak.status,
                    fnr = emptyList(),
                    opprettetTidspunkt = sak.opprettetTidspunkt
                ),
                tilkjent = hentTilkjentYtelse(behandling.id)
            )
        }


    }

    fun hentBehandling(sakId: Long): BehandlingDB? {
        return connection.queryFirstOrNull(
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
                UnderveisDTO(
                    underveisFom = row.getLocalDate("PERIODE_FOM"),
                    underveisTom = row.getLocalDate("PERIODE_TOM"),
                    meldeperiodeFom = row.getLocalDate("MELDEPERIODE_FOM"),
                    meldeperiodeTom = row.getLocalDate("MELDEPERIODE_TOM"),
                    utfall = row.getString("UTFALL"),
                    rettighetsType = RettighetsType.valueOf(row.getString("RETTIGHETS_TYPE")),
                    avslagsårsak = row.getString("AVSLAGSÅRSAK")
                )
            }
        }
    }

    fun hentTilkjentYtelse(behandlingId: Long): List<TilkjentDTO> {
        return connection.queryList(
            """
                SELECT * FROM TILOKJENT_PERIODE
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
                    tilkjentTom = it.getPeriode("PERIODE").fom,
                    dagsats = it.getInt("DAGSATS"),
                    gradering = it.getInt("GRADERING"),
                    grunnlag = it.getInt("GRUNNLAG"),
                    grunnlagsfaktor = it.getInt("GRUNNLAGSFAKTOR"),
                    grunnbeløp = it.getInt("GRUNNBELØP"),
                    antallBarn = it.getInt("ANTALL_BARN"),
                    barnetilleggsats = it.getInt("BARNETILLEGGSATS"),
                    barnetillegg = it.getInt("BARNETILLEGG")
                )
            }
        }
    }
}

data class sakDB(
    val id: Long,
    val status: no.nav.aap.behandlingsflyt.kontrakt.sak.Status,
    val rettighetsPeriode: Periode,
    val saksnummer: String,
    val opprettetTidspunkt: LocalDateTime
)

data class BehandlingDB(
    val id: Long,
    val behandlingStatus: no.nav.aap.behandlingsflyt.kontrakt.behandling.Status,
    val vedtaksDato: LocalDate,
    val type: String,
    val oppretterTidspunkt: LocalDate,
)

fun mergeTilkjentPeriods(periods: List<TilkjentDTO>): List<TilkjentDTO> {
    if (periods.isEmpty()) return emptyList()

    val sortedPeriods = periods.sortedBy { it.tilkjentFom }
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