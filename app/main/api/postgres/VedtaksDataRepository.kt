package api.postgres

import api.maksimum.UtbetalingMedMer
import api.maksimum.Vedtak
import api.maksimum.VedtakDataKelvin
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class VedtaksDataRepository(val connection: DBConnection) {
    fun lagre(kelvin: VedtakDataKelvin) {
        connection.executeBatch(
            """
                DELETE FROM VEDTAK
                WHERE VEDTAKSID = ?
            """.trimIndent(),
            kelvin.maksimum.vedtak
        ) {
            setParams {
                setString(1, it.vedtaksId)
            }
        }

        kelvin.maksimum.vedtak.forEach { vedtak ->
            val utbetalingVedtakId = connection.executeReturnKey(
                """
                INSERT INTO VEDTAK (FNR, VEDTAKSID,DAGSATS, STATUS, SAKSNUMMER, VEDTAKSDATO, RETTIGHETS_TYPE, BEREGNINGSGRUNNLAG, BARN_MED_STONAD, SAMORDNINGS_ID, OPPHORSORSAK, VEDTAKS_TYPE_KODE, VEDTAKS_TYPE_NAVN, PERIODE )
                VALUES (?, ?, ?, ? , ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::daterange)
            """.trimIndent(),

                ) {
                setParams {
                    setString(1, kelvin.fnr)
                    setString(2, vedtak.vedtaksId)
                    setInt(3, vedtak.dagsats)
                    setString(4, vedtak.status)
                    setString(5, vedtak.saksnummer)
                    setLocalDate(6, LocalDate.parse(vedtak.vedtaksdato))
                    setString(7, vedtak.rettighetsType)
                    setInt(8, vedtak.beregningsgrunnlag)
                    setInt(9, vedtak.barnMedStonad)
                    setString(10, vedtak.samordningsId)
                    setString(11, vedtak.opphorsAarsak)
                    setString(12, vedtak.vedtaksTypeKode)
                    setString(13, vedtak.vedtaksTypeNavn)
                    setPeriode(14, vedtak.periode.toKelvinPeriode())
                }
            }
            lagreUtbetaling(utbetalingVedtakId, vedtak.utbetaling)
        }

    }

    fun lagreUtbetaling(vedtakId: Long, utbetalinger: List<UtbetalingMedMer>) {
        connection.executeBatch(
            """
                INSERT INTO UTBETALING (VEDTAK_ID, UTBETALINGSGRAD, BELOP, DAGSATS, BARNETILLEGG, PERIODE)
                VALUES (?, ?, ?, ?, ?, ?::daterange)
            """.trimIndent(),
            utbetalinger
        ) {
            setParams { utbetaling ->
                setLong(1, vedtakId)
                setInt(2, utbetaling.utbetalingsgrad)
                setInt(3, utbetaling.belop)
                setInt(4, utbetaling.dagsats)
                setInt(5, utbetaling.barnetilegg)
                setPeriode(6, utbetaling.periode.toKelvinPeriode())
            }
        }
    }

    fun hentHvisEksisterer(fnr: String): List<Vedtak> {
        return connection.queryList(
            """
                SELECT * FROM VEDTAK
                WHERE FNR = ?
            """.trimIndent()
        ){
            setParams {
                setString(1, fnr)
            }
            setRowMapper { row ->
                Vedtak(
                    row.getString("VEDTAKSID"),
                    row.getInt("DAGSATS"),
                    row.getString("STATUS"),
                    row.getString("SAKSNUMMER"),
                    row.getLocalDate("VEDTAKSDATO").format(DateTimeFormatter.ISO_LOCAL_DATE),
                    row.getPeriode("PERIODE").toDatadelingPeriode(),
                    row.getString("RETTIGHETS_TYPE"),
                    row.getInt("BEREGNINGSGRUNNLAG"),
                    row.getInt("BARN_MED_STONAD"),
                    "Kelvin",
                    row.getStringOrNull("SAMORDNINGS_ID"),
                    row.getStringOrNull("OPPHORSORSAK"),
                    row.getString("VEDTAKS_TYPE_KODE"),
                    row.getString("VEDTAKS_TYPE_NAVN"),
                    hentUtbetalinger(row.getLong("ID"))
                )
            }
        }.toList()
    }

    fun hentUtbetalinger(vedtakId: Long): List<UtbetalingMedMer> {
        return connection.queryList(
            """
                SELECT * FROM UTBETALING
                WHERE VEDTAK_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, vedtakId)
            }
            setRowMapper { row ->
                UtbetalingMedMer(
                    null,
                    row.getInt("UTBETALINGSGRAD"),
                    row.getPeriode("PERIODE").toDatadelingPeriode(),
                    row.getInt("BELOP"),
                    row.getInt("DAGSATS"),
                    row.getInt("BARNETILLEGG")
                )
            }
        }.toList()
    }


    private fun no.nav.aap.api.intern.Periode.toKelvinPeriode(): Periode {
        return Periode(
            this.fraOgMedDato!!,
            this.tilOgMedDato!!
        )
    }
    private fun Periode.toDatadelingPeriode(): no.nav.aap.api.intern.Periode {
        return no.nav.aap.api.intern.Periode(
            this.fom,
            this.tom
        )
    }
}