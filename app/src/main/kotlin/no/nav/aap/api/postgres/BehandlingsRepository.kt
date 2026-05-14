package no.nav.aap.api.postgres

import no.nav.aap.api.kelvin.*
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.ZoneOffset

class BehandlingsRepository(private val connection: DBConnection) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun lagreBehandling(fnr: List<String>, behandling: Behandling) {
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
                INSERT INTO SAK (RETTIGHETSPERIODE, SAKSNUMMER)
                VALUES (?::daterange, ?)
            """.trimIndent()
        ) {
            setParams {
                setPeriode(1, behandling.rettighetsperiode)
                setString(2, behandling.sak.saksnummer)
            }
        }

        connection.executeBatch(
            """DELETE FROM SAK_PERSON WHERE SAK_ID = ? AND PERSON_IDENT = ?""".trimIndent(),
            fnr
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
            fnr
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

        connection.execute(
            """
                DELETE FROM stans_opphor_grunnlag WHERE behandling_id = ?
            """.trimIndent(),
        ) {
            setParams {
                setLong(1, nyBehandlingId)
            }
        }

        val stansGrunnlagId = connection.executeReturnKey(
            """INSERT INTO stans_opphor_grunnlag (behandling_id, opprettet_tid)
       VALUES (?, now())""".trimIndent()
        ) {
            setParams {
                setLong(1, nyBehandlingId)
            }
        }

        behandling.stansOpphørVurdering?.forEach { vurdering ->
            val vurderingId = connection.executeReturnKey(
                """INSERT INTO stans_opphor_vurdering (stans_opphor_grunnlag_id, fom, vedtakstype, opprettet_tid) VALUES (?, ?, ?, ?)""".trimIndent()
            ) {
                setParams {
                    setLong(1, stansGrunnlagId)
                    setLocalDate(2, vurdering.fom)
                    setString(3, vurdering.vurdering.name)
                    setInstant(4, vurdering.opprettet)
                }
            }
            connection.executeBatch(
                """INSERT INTO avslagsarsak (stans_opphor_vurdering_id, avslagsarsak) VALUES (?, ?)""".trimIndent(),
                vurdering.avslagsårsaker
            ) {
                setParams {
                    setLong(1, vurderingId)
                    setEnumName(2, it)
                }
            }
        }

        connection.executeBatch(
            """
                INSERT INTO RETTIGHETSTYPE (BEHANDLING_ID, PERIODE, RETTIGHETSTYPE)
                VALUES (?, ?::daterange, ?)
            """.trimIndent(),
            behandling.rettighetsTypePerioder
        ) {
            setParams {
                setLong(1, nyBehandlingId)
                setPeriode(2, it.periode)
                setString(3, it.verdi)
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
            behandling.tilkjent.segmenter()
        ) {
            setParams {
                setLong(1, nytilkjentId)
                setPeriode(2, it.periode)
                setBigDecimal(3, it.verdi.dagsats.toBigDecimal())
                setInt(4, it.verdi.gradering)
                setBigDecimal(5, it.verdi.grunnlagsfaktor)
                setBigDecimal(6, it.verdi.grunnbeløp)
                setInt(7, it.verdi.antallBarn)
                setBigDecimal(8, it.verdi.barnetilleggsats)
                setBigDecimal(9, it.verdi.barnetillegg)
                setInt(10, it.verdi.samordningUføregradering)
            }
        }
        if (behandling.beregningsgrunnlag != null) {
            connection.execute(
                """INSERT INTO BEREGNINGSGRUNNLAG (BEHANDLING_ID, BELOP) VALUES (?, ?)""".trimIndent()
            ) {
                setParams {
                    setLong(1, nyBehandlingId)
                    setBigDecimal(2, behandling.beregningsgrunnlag)
                }
            }
        } else {
            connection.execute("""DELETE FROM BEREGNINGSGRUNNLAG WHERE behandling_id=?""".trimIndent()) {
                setParams {
                    setLong(1, nyBehandlingId)
                }
            }
        }

        connection.execute("DELETE FROM ARENAVEDTAK WHERE behandling_id = ?") {
            setParams {
                setLong(1, nyBehandlingId)
            }
        }
        connection.executeBatch(
            """
            INSERT INTO ARENAVEDTAK(behandling_id, vedtak_id, vedtaksvariant, fom, tom) VALUES (?, ?, ?, ?, ?)
        """,
           behandling.arenakompatibleVedtak,
        ) {
            setParams {
                setLong(1, nyBehandlingId)
                setLong(2, it.vedtakId)
                setEnumName(3, it.vedtaksvariant)
                setLocalDate(4, it.fom)
                setLocalDate(5, it.tom)
            }
        }
    }

    fun hentVedtaksData(fnr: String, periode: Periode): List<Behandling> {
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
                        rettighetsPeriode = row.getPeriode("RETTIGHETSPERIODE"),
                        id = it
                    )
                }
            }
        }

        return saker.flatMap { sak ->
            hentBehandlinger(sak)
        }
    }

    private fun hentArenavedtak(behandlingId: Long): List<Arenavedtak> {
        return connection.queryList("""
            SELECT * FROM ARENAVEDTAK WHERE behandling_id = ?
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId)
            }
            setRowMapper {
                Arenavedtak(
                    vedtakId = it.getLong("vedtak_id"),
                    fom = it.getLocalDate("fom"),
                    tom = it.getLocalDate("tom"),
                    vedtaksvariant = it.getEnum("vedtaksvariant"),
                )
            }
        }.sortedBy { it.fom }
    }
    private fun hentBeregningsGrunnlag(behandlingId: Long): BigDecimal? {
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

    private fun hentRettighetsTypePerioder(behandlingId: Long): List<RettighetsTypePeriode> {
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

    private fun hentBehandlinger(sak: SakDB): List<Behandling> {
        return connection.queryList(
            """
                SELECT * FROM BEHANDLING
                WHERE SAK_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, sak.id)
            }
            setRowMapper { row ->
                val behandlingId = row.getLong("ID")
                Behandling(
                    behandlingStatus = row.getEnum("STATUS"),
                    vedtaksDato = row.getLocalDate("VEDTAKS_DATO"),
                    sak = Sak(
                        saksnummer = sak.saksnummer,
                        opprettetTidspunkt = row.getLocalDateTime("OPPRETTET_TID"),
                    ),
                    behandlingsReferanse = row.getString("BEHANDLING_REFERANSE"),
                    samId = row.getStringOrNull("SAMID"),
                    vedtakId = row.getLongOrNull("VEDTAKID") ?: 0L,
                    nyttVedtak = row.getBoolean("NYTT_VEDTAK"),
                    tilkjent = hentTilkjentYtelse(behandlingId),
                    rettighetsTypePerioder = hentRettighetsTypePerioder(behandlingId),
                    beregningsgrunnlag = hentBeregningsGrunnlag(behandlingId)
                        ?: BigDecimal.ZERO, // TODO!!!
                    stansOpphørVurdering = hentStansOpphør(behandlingId),
                    rettighetsperiode = sak.rettighetsPeriode,
                    arenakompatibleVedtak = hentArenavedtak(behandlingId),
                )
            }
        }
    }

    private fun hentStansOpphør(behandlingId: Long): Set<GjeldendeStansEllerOpphør> {
        return connection.queryList(
            """SELECT * FROM stans_opphor_vurdering WHERE stans_opphor_grunnlag_id IN (SELECT id FROM stans_opphor_grunnlag WHERE behandling_id = ?)""".trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId)
            }
            setRowMapper {

                GjeldendeStansEllerOpphør(
                    fom = it.getLocalDate("fom"),
                    opprettet = it.getLocalDateTime("opprettet_tid")
                        .toInstant(ZoneOffset.UTC),
                    vurdering = StansEllerOpphør.valueOf(it.getString("vedtakstype")),
                    avslagsårsaker = hentAvslagsårsaker(it.getLong("id"))
                )
            }
        }.toSet()
    }

    private fun hentAvslagsårsaker(stansOpphørVurderingId: Long): Set<Avslagsårsak> {
        return connection.queryList(
            """SELECT * FROM avslagsarsak WHERE stans_opphor_vurdering_id = ?""".trimIndent()
        ) {
            setParams {
                setLong(1, stansOpphørVurderingId)
            }
            setRowMapper { row ->
                row.getEnum<Avslagsårsak>("avslagsarsak")
            }
        }.toSet()
    }

    private fun hentTilkjentYtelse(behandlingId: Long): Tidslinje<TilkjentYtelse> {
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
                Segment(
                    it.getPeriode("PERIODE"),
                    TilkjentYtelse(
                        dagsats = it.getBigDecimal("DAGSATS").toInt(),
                        gradering = it.getInt("GRADERING"),
                        grunnlagsfaktor = it.getBigDecimal("GRUNNLAGSFAKTOR"),
                        grunnbeløp = it.getBigDecimal("GRUNNBELOP"),
                        antallBarn = it.getInt("ANTALL_BARN"),
                        barnetilleggsats = it.getBigDecimal("BARNETILLEGGSATS"),
                        barnetillegg = it.getBigDecimal("BARNETILLEGG"),
                        samordningUføregradering = it.getIntOrNull("UFOREGRADERING")
                    )
                )
            }
        }.let { Tidslinje(it) }
    }

    fun erNyttVedtak(fnr: String): Boolean {
        return connection.queryList(
            """
                SELECT SAK_ID FROM SAK_PERSON
                WHERE PERSON_IDENT = ?
            """.trimIndent()
        ) {
            setParams { setString(1, fnr) }
            setRowMapper { row -> row.getLong("SAK_ID") }
        }.isEmpty()
    }

    fun lagreOppdaterteIdenter(saksnummer: String, identer: List<String>) {
        val saker = connection.queryList("SELECT * FROM SAK WHERE saksnummer = ?") {
            setParams { setString(1, saksnummer) }
            setRowMapper { it.getLong("id") }
        }

        if (saker.isEmpty()) {
            log.warn("Fant ingen saker med saksnummer $saksnummer, kan ikke oppdatere identer. Dette er forventet om det ikke finnes vedtak på sak.")
            return
        }
        require(saker.size == 1) { "Fant flere saker med saksnummer $saksnummer, kan ikke oppdatere identer" }

        val sakId = saker.single()

        connection.executeBatch(
            """
                INSERT INTO SAK_PERSON (SAK_ID, PERSON_IDENT)
                VALUES (?, ?)
                ON CONFLICT (SAK_ID, PERSON_IDENT) DO NOTHING 
            """.trimIndent(),
            identer
        ) {
            setParams {
                setLong(1, sakId)
                setString(2, it)
            }
        }
    }
}

private data class SakDB(
    val id: Long,
    val rettighetsPeriode: Periode,
    val saksnummer: String,
)