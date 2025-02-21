package api.postgres

import api.kelvin.MeldekortPerioderDTO
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode

class MeldekortPerioderRepository(private val connection: DBConnection) {
    fun lagreMeldekortPerioder(fnr: String, perioder: List<Periode>): List<Periode> {
        connection.execute(
            """
                DELETE FROM MELDEKORT_PERIODER_MED_FNR
                WHERE PERSONIDENT = ?
                """.trimIndent()
        ) {
            setParams {
                setString(1, fnr)
            }
        }

        connection.executeBatch(
            """
                INSERT INTO MELDEKORT_PERIODER_MED_FNR (PERSONIDENT, periode)
                VALUES (?, ?::daterange)
            """.trimIndent(),
            perioder
        ) {
            setParams {
                setString(1, fnr)
                setPeriode(2, it)
            }
        }

        return perioder

    }


    fun hentMeldekortPerioder(fnr: String): MeldekortPerioderDTO {
        return MeldekortPerioderDTO(
            fnr,
            connection.queryList(
                """
                    SELECT PERIODE
                    FROM MELDEKORT_PERIODER_MED_FNR
                    WHERE PERSONIDENT = ?
                """.trimIndent()
            ) {
                setParams {
                    setString(1, fnr)
                }
                setRowMapper { row ->
                    row.getPeriode("periode")
                }
            }.toList()
        )
    }
}