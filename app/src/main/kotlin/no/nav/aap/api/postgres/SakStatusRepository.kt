package no.nav.aap.api.postgres

import no.nav.aap.api.intern.behandlingsflyt.SakStatus
import no.nav.aap.api.intern.behandlingsflyt.SakstatusFraKelvin
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode

class SakStatusRepository(private val connection: DBConnection) {
    fun lagreSakStatusFraKelvin(fnr: String, sakStatus: SakStatus) {
        connection.execute(
            """
                DELETE FROM SAKER
                WHERE SAKSNUMMER = ?
            """.trimIndent()
        ) {
            setParams {
                setString(1, sakStatus.sakId)
            }
        }


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
                setPeriode(4, sakStatus.periode.tilKelvinPeriode())
            }
        }
    }

    fun hentSakStatus(fnr: String): List<SakStatus> {
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
                SakStatus(
                    sakId = row.getString("SAKSNUMMER"),
                    statusKode = SakstatusFraKelvin.valueOf(row.getString("STATUS")),
                    periode = row.getPeriode("RETTIGHETS_PERIODE")
                        .let { no.nav.aap.api.intern.behandlingsflyt.Periode(it.fom, it.tom) },
                )
            }
        }
    }

    private fun Periode.toKontraktPeriode(): no.nav.aap.api.intern.Periode {
        return no.nav.aap.api.intern.Periode(this.fom, this.tom)
    }

    private fun no.nav.aap.api.intern.behandlingsflyt.Periode.tilKelvinPeriode(): Periode {
        return Periode(this.fom, this.tom)
    }
}