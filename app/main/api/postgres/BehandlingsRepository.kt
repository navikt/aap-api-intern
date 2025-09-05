package api.postgres

import com.papsign.ktor.openapigen.annotations.properties.description.Description
import no.nav.aap.api.intern.VedtakUtenUtbetaling
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class BehandlingsRepository(private val connection: DBConnection) {
    private val log = LoggerFactory.getLogger(javaClass)

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

        log.info("Lagrer behandling for sak: ${behandling.sak.saksnummer}, gammel sak id: $gammelSak")

        val sakId = gammelSak ?: connection.executeReturnKey(
            """
                INSERT INTO SAK (STATUS, RETTIGHETSPERIODE, SAKSNUMMER)
                VALUES (?, ?::daterange, ?)
            """.trimIndent()
        ) {
            setParams {
                setString(1, behandling.sak.status.toString())
                setPeriode(
                    2,
                    Periode(behandling.rettighetsPeriodeFom, behandling.rettighetsPeriodeTom)
                )
                setString(3, behandling.sak.saksnummer)
            }
        }

        connection.executeBatch(
            """DELETE FROM SAK_PERSON WHERE SAK_ID = ? AND PERSON_IDENT = ?""".trimIndent(),
            behandling.sak.fnr
        ) {
            setParams {
                setLong(1, sakId)
                setString(2, it)
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

        val nyBehandlingId = connection.queryFirst(
            """
                INSERT INTO BEHANDLING (SAK_ID, STATUS, VEDTAKS_DATO, OPPRETTET_TID, BEHANDLING_REFERANSE,
                                        SAMID, VEDTAKID)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (sak_id) DO UPDATE SET STATUS        = EXCLUDED.status,
                                                   vedtaks_dato  = excluded.vedtaks_dato,
                                                   OPPRETTET_TID = excluded.opprettet_tid
                RETURNING ID
            """.trimIndent()
        ) {
            setParams {
                setLong(1, sakId)
                setString(2, behandling.behandlingStatus.toString())
                setLocalDate(3, behandling.vedtaksDato)
                setLocalDateTime(4, behandling.sak.opprettetTidspunkt)
                setString(5, behandling.behandlingsReferanse)
                setString(6, behandling.samId)
                setLong(7, behandling.vedtakId)
            }

            setRowMapper { row ->
                row.getLong("ID")
            }
        }

        connection.execute(
            """
                DELETE FROM RETTIGHETSTYPE WHERE BEHANDLING_ID = ?
            """.trimIndent(),
        ) {
            setParams {
                setLong(1, nyBehandlingId)
            }
        }

        connection.executeBatch(
            """
                INSERT INTO RETTIGHETSTYPE (BEHANDLING_ID, PERIODE, RETTIGHETSTYPE)
                VALUES (?, ?::daterange, ?)
            """.trimIndent(),
            behandling.rettighetsTypeTidsLinje
        ) {
            setParams {
                setLong(1, nyBehandlingId)
                setPeriode(2, Periode(it.fom, it.tom))
                setString(3, it.verdi)
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
                INSERT INTO TILKJENT_PERIODE (TILKJENT_YTELSE_ID, PERIODE, DAGSATS, GRADERING,
                                              GRUNNLAGSFAKTOR, GRUNNBELOP, ANTALL_BARN, BARNETILLEGGSATS,
                                              BARNETILLEGG, UFOREGRADERING)
                VALUES (?, ?::daterange, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            behandling.tilkjent
        ) {
            setParams {
                setLong(1, nytilkjentId)
                setPeriode(2, Periode(it.tilkjentFom, it.tilkjentTom))
                setBigDecimal(3, it.dagsats.toBigDecimal())
                setInt(4, it.gradering)
                setBigDecimal(5, it.grunnlagsfaktor)
                setBigDecimal(6, it.grunnbeløp)
                setInt(7, it.antallBarn)
                setBigDecimal(8, it.barnetilleggsats)
                setBigDecimal(9, it.barnetillegg)
                setInt(10, it.samordningUføregradering)
            }
        }
        connection.execute(
            """INSERT INTO BEREGNINGSGRUNNLAG (BEHANDLING_ID, BELOP) VALUES (?, ?)""".trimIndent()
        ) {
            setParams {
                setLong(1, nyBehandlingId)
                setBigDecimal(2, behandling.beregningsgrunnlag)
            }
        }

    }

    // TODO: ikke returner DTO fra behandlingsflyt her, heller dupliser i kode her
    // Kommer til å bli kronglete ved modellendringer
    fun hentVedtaksData(fnr: String, periode: Periode): List<DatadelingDTO> {
        val sakerIder = connection.queryList(
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
        }.toSet()

        val saker = sakerIder.mapNotNull {
            connection.queryFirstOrNull<SakDB>(
                """
                    SELECT * FROM SAK
                    WHERE ID = ? AND RETTIGHETSPERIODE && ?::daterange
                """.trimIndent()
            ) {
                setParams {
                    setLong(1, it)
                    setPeriode(2, periode)
                }
                setRowMapper { row ->
                    SakDB(
                        saksnummer = row.getString("SAKSNUMMER"),
                        status = KelvinSakStatus.valueOf(row.getString("STATUS")),
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
                    behandlingsReferanse = behandling.behandlingReferanse,
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
                    tilkjent = hentTilkjentYtelse(behandling.id),
                    rettighetsTypeTidsLinje = hentRettighetsTypeTidslinje(behandling.id),
                    samId = behandling.samid,
                    vedtakId = behandling.vedtakId ?: 0L,
                    beregningsgrunnlag = hentBeregningsGrunnlag(behandling.id)
                        ?: BigDecimal.ZERO, // TODO!!!
                )
            }
        }


    }

    fun hentBeregningsGrunnlag(behandlingId: Long): BigDecimal? {
        return connection.queryFirstOrNull(
            """
                SELECT BELOP FROM BEREGNINGSGRUNNLAG
                WHERE BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId)
            }
            setRowMapper { row ->
                row.getBigDecimal("BELOP")
            }
        }
    }

    fun hentRettighetsTypeTidslinje(behandlingId: Long): List<RettighetsTypePeriode> {
        return connection.queryList(
            """
                SELECT * FROM RETTIGHETSTYPE
                WHERE BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId)
            }
            setRowMapper {
                val periode = it.getPeriode("PERIODE")
                RettighetsTypePeriode(
                    fom = periode.fom,
                    tom = periode.tom,
                    verdi = it.getString("RETTIGHETSTYPE")
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
                    behandlingStatus = row.getEnum("STATUS"),
                    vedtaksDato = row.getLocalDate("VEDTAKS_DATO"),
                    opprettetTidspunkt = row.getLocalDate("OPPRETTET_TID"),
                    behandlingReferanse = row.getString("BEHANDLING_REFERANSE"),
                    samid = row.getStringOrNull("SAMID"),
                    vedtakId = row.getLongOrNull("VEDTAKID")
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
                    grunnlagsfaktor = it.getBigDecimal("GRUNNLAGSFAKTOR"),
                    grunnbeløp = it.getBigDecimal("GRUNNBELOP"),
                    antallBarn = it.getInt("ANTALL_BARN"),
                    barnetilleggsats = it.getBigDecimal("BARNETILLEGGSATS"),
                    barnetillegg = it.getBigDecimal("BARNETILLEGG"),
                    samordningUføregradering = 100 - (it.getIntOrNull("UFOREGRADERING") ?: 0)
                )
            }
        }
    }
}

data class SakDB(
    val id: Long,
    val status: KelvinSakStatus,
    val rettighetsPeriode: Periode,
    val saksnummer: String,
)

data class BehandlingDB(
    val id: Long,
    val behandlingStatus: KelvinBehandlingStatus,
    val vedtaksDato: LocalDate,
    val opprettetTidspunkt: LocalDate,
    val behandlingReferanse: String,
    val samid: String? = null,
    val vedtakId: Long? = null
)

data class TilkjentDB(
    val dagsats: Int,
    val gradering: Int,
    val grunnlagsfaktor: BigDecimal,
    val grunnbeløp: BigDecimal,
    val antallBarn: Int,
    val barnetilleggsats: BigDecimal,
    val barnetillegg: BigDecimal,
    val uføregrad: Int? = 0,
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

/**
 * @param vedtakId Svarer til ID til vedtak-tabellen i behandlingsflyt.
 */
data class VedtakUtenUtbetalingUtenPeriode(
    val vedtakId: String,
    val dagsats: Int,
    @param:Description("Dagsats etter uføre-reduksjon. Dette er lik dagsats * (100 - uføregrad) / 100. Kommer kun fra nytt system (Kelvin). Ved manglende data er denne null.")
    val dagsatsEtterUføreReduksjon: Int,
    @param:Description("Status på et vedtak. Mulige verdier er LØPENDE, AVSLUTTET, UTREDES.")
    val status: String, //Hypotese, vedtaksstatuskode
    val saksnummer: String,
    val vedtaksdato: LocalDate, //reg_dato
    @param:Description("Rettighetsgruppe. For data fra Arena er dette aktivitetsfasekode.")
    val rettighetsType: String, ////aktivitetsfase //Aktfasekode
    val beregningsgrunnlag: Int,
    val barnMedStonad: Int,
    @param:Description("Kildesystem for vedtak. Mulige verdier er ARENA og KELVIN.")
    val kildesystem: String = "ARENA",
    val samordningsId: String? = null,
    val opphorsAarsak: String? = null,
    val barnetilleggSats: BigDecimal? = null
) {
    fun tilVedtakUtenUtbetaling(periode: no.nav.aap.api.intern.Periode): VedtakUtenUtbetaling {
        return VedtakUtenUtbetaling(
            vedtakId = this.vedtakId,
            dagsats = this.dagsats,
            dagsatsEtterUføreReduksjon = this.dagsatsEtterUføreReduksjon,
            status = this.status,
            saksnummer = this.saksnummer,
            vedtaksdato = this.vedtaksdato,
            vedtaksTypeKode = null,
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