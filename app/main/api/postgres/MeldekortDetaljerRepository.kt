package api.postgres

import api.kelvin.MeldekortDTO
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import java.time.LocalDate

class MeldekortDetaljerRepository(private val connection: DBConnection) {

    fun lagreListeMedMeldekort(meldekort: List<MeldekortDTO>) {
        connection.execute(
            """
                DELETE FROM MELDEKORT
                WHERE SAKSNUMMER = ?
            """.trimIndent()
        ) {
            setParams {
                setString(1, meldekort.first().saksnummer.toString())
            }
        }

        meldekort.forEach {
            val meldekortId = insertMeldekort(connection, it)
            insertTimerArbeider(connection, meldekortId,it.arbeidPerDag)
        }

    }

    private fun insertMeldekort(connection: DBConnection, meldekort: MeldekortDTO): Long {
        return connection.executeReturnKey(
            """INSERT INTO MELDEKORT(PERSON_IDENT, MOTTATT_TIDSPUNKT, SAKSNUMMER, PERIODE, MELDEPLIKTSTATUSKODE, RETTIGHETSTYPEKODE) VALUES (?, ?, ?, ?::daterange, ?, ?)"""
        ){
            setParams {
                setString(1, meldekort.personIdent)
                setLocalDateTime(2, meldekort.mottattTidspunkt)
                setString(3, meldekort.saksnummer)
                setPeriode(4, meldekort.meldePeriode)
                setString(5, meldekort.meldepliktStatusKode)
                setString(6, meldekort.rettighetsTypeKode)
            }
        }
    }

    private fun insertTimerArbeider(connection: DBConnection, meldekortId: Long, timerArbeid: List<MeldekortDTO.MeldeDag>) {
        connection.executeBatch(
            """INSERT INTO MELDEKORT_ARBEIDS_PERIODE(MELDEKORT_ID, DATO, TIMER_ARBEIDET) VALUES (?, ?, ?)""".trimIndent(),
            timerArbeid
        ){
            setParams {it ->
                setLong(1, meldekortId)
                setLocalDate(2, it.dag)
                setBigDecimal(3, it.timerArbeidet)
            }
        }
        println("done")
    }

    fun hentAlle(personIdentifikatorer: List<String>, fom: LocalDate? = null, tom: LocalDate? = null): List<MeldekortDTO> {
        val iMorgen = LocalDate.now().plusDays(1)
        require(fom == null || fom.isBefore(iMorgen)) {
            "Kan ikke hente meldekort fra og med en fremtidig dato"
        }
        return connection.queryList<MeldekortDTO>(
            """SELECT * FROM MELDEKORT WHERE PERSON_IDENT = ANY(?)
                AND ( ? IS NULL OR MOTTATT_TIDSPUNKT >= ?::date) 
                AND ( ? IS NULL OR MOTTATT_TIDSPUNKT <= ?::date)
            """.trimIndent()
        ){
            setParams {
                setArray(1, personIdentifikatorer)
                setLocalDate(2, fom)
                setLocalDate(3, fom)
                setLocalDate(4, tom)
                setLocalDate(5, tom)
            }
            setRowMapper { row ->
                rowToMeldekortDTO(row)
            }
        }
    }

    private fun rowToMeldekortDTO(row: Row): MeldekortDTO{
        return MeldekortDTO(
            personIdent = row.getString("PERSON_IDENT"),
            saksnummer = row.getString("SAKSNUMMER"),
            mottattTidspunkt = row.getLocalDateTime("MOTTATT_TIDSPUNKT"),
            meldePeriode = row.getPeriode("PERIODE"), // midlertidig, overskrives under mapping
            arbeidPerDag = hentArbeidPerDag(row.getLong("ID")), // midlertidig, overskrives under mapping
            meldepliktStatusKode = row.getStringOrNull("MELDEPLIKTSTATUSKODE"),
            rettighetsTypeKode = row.getStringOrNull("RETTIGHETSTYPEKODE"),
            avslagsårsakKode = null,
        )
    }

    private fun hentArbeidPerDag(meldekortId: Long): List<MeldekortDTO.MeldeDag> {
        return connection.queryList(
            """SELECT DATO, TIMER_ARBEIDET FROM MELDEKORT_ARBEIDS_PERIODE
                WHERE MELDEKORT_ID = ?
                ORDER BY DATO
            """.trimIndent()
        ){
            setParams {
                setLong(1, meldekortId)
            }
            setRowMapper { row ->
                MeldekortDTO.MeldeDag(
                    dag = row.getLocalDate("DATO"),
                    timerArbeidet = row.getBigDecimal("TIMER_ARBEIDET")
                )
            }
        }
    }

}