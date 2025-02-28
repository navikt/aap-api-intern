package api.postgres

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode

class SakStatusRepository(private val connection: DBConnection) {
    fun lagreSakStatus(fnr: String, sakStatus: no.nav.aap.api.intern.SakStatus) {
        connection.execute(
            """
                INSERT INTO SAKER (IDENT, SAKSNUMMER, STATUS, RETTIGHETS_PERIODE)
                VALUES (?, ?, ?, ?::daterange)
            """.trimIndent()
        ) {
            setParams {
                setString(1, fnr)
                setString(2, sakStatus.sakId)
                setString(3, sakStatus.statusKode.toString())
                setPeriode(4, sakStatus.periode.toPeriode())
            }
        }
    }

    fun hentSakStatus(fnr: String):List<no.nav.aap.api.intern.SakStatus>{
        return connection.queryList(
            """
                SELECT SAKSNUMMER, STATUS, RETTIGHETS_PERIODE
                FROM SAKER
                WHERE IDENT = ?
            """.trimIndent()
        ) {
            setParams {
                setString(1, fnr)
            }
            setRowMapper { row ->
                no.nav.aap.api.intern.SakStatus(
                sakId = row.getString("SAKSNUMMER"),
                statusKode = no.nav.aap.api.intern.Status.valueOf(row.getString("STATUS")),
                periode = row.getPeriode("RETTIGHETS_PERIODE").toKontraktPeriode(),
                )
            }
        }
    }

    private fun no.nav.aap.api.intern.Periode.toPeriode(): Periode {
        return Periode(this.fraOgMedDato!!, this.tilOgMedDato!!)
    }

    private fun Periode.toKontraktPeriode(): no.nav.aap.api.intern.Periode {
        return no.nav.aap.api.intern.Periode(this.fom, this.tom)
    }
}