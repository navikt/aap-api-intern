package no.nav.aap.api.postgres

import java.time.LocalDate
import no.nav.aap.api.intern.behandlingsflyt.SakStatus
import no.nav.aap.api.intern.behandlingsflyt.SakstatusFraKelvin
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode

class SakStatusRepository(private val connection: DBConnection) {
    fun lagreSakStatusFraKelvin(fnr: String, sakStatus: SakStatus) {
        val søknadsdatoer = sakStatus.søknadsdatoer.orEmpty().map(LocalDate::toString)

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
                INSERT INTO SAKER (IDENT, SAKSNUMMER, STATUS, RETTIGHETS_PERIODE, SOKNADSDATOER)
                VALUES (?, ?, ?, ?::daterange, ?::date[])
            """.trimIndent()
        ) {
            setParams {
                setString(1, fnr)
                setString(2, sakStatus.sakId)
                setString(3, sakStatus.statusKode.toString())
                setPeriode(4, sakStatus.periode.tilKelvinPeriode())
                setArray(5, søknadsdatoer)
            }
        }
    }

    fun hentSakStatus(fnr: String): List<SakStatus> {
        return connection.queryList(
            """
                SELECT SAKSNUMMER, STATUS, RETTIGHETS_PERIODE, SOKNADSDATOER
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
                    søknadsdatoer = row.getArray("SOKNADSDATOER", java.sql.Date::class)
                        .map { it.toLocalDate() },
                    statusKode = SakstatusFraKelvin.valueOf(row.getString("STATUS")),
                    periode = row.getPeriode("RETTIGHETS_PERIODE")
                        .let { no.nav.aap.api.intern.behandlingsflyt.Periode(it.fom, it.tom) },
                )
            }
        }
    }

    private fun no.nav.aap.api.intern.behandlingsflyt.Periode.tilKelvinPeriode(): Periode {
        return Periode(this.fom, this.tom)
    }
}