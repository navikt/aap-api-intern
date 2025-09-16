package api.postgres

import api.kelvin.MeldekortDTO
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.TimerArbeid
import java.time.LocalDate

class MeldekortDetaljerRepository(private val connection: DBConnection) {

    fun lagre(meldekort: List<MeldekortDTO>, identer: List<String>) {
    // dersom lagringen feiler, må vi kaste exception for at transaksjonen skal rulles tilbake
        connection.execute(
            """
                DELETE FROM MELDEKORT
                WHERE PERSONIDENT IN ?
            """.trimIndent()
        ) {
            setParams {
                setArray(1, identer)
            }
        }

        meldekort.forEach {
            val meldekortId = insertMeldekort(connection, it)
            insertTimerArbeider(connection, meldekortId,it.arbeidPerDag)
        }
    }

    private fun insertMeldekort(connection: DBConnection, meldekort: MeldekortDTO): Long {
        return connection.executeReturnKey(
            """INSERT INTO MELDEKORT(PERSONIDENT, MOTTATT_TIDSPUNKT, SAKSNUMMER) VALUES (?, ?, ?)"""
        ){
            setParams {
                setString(1, meldekort.personIdent)
                setLocalDateTime(2, meldekort.mottattTidspunkt)
                setString(3, meldekort.saksnummer.toString())
            }
        }
    }

    private fun insertTimerArbeider(connection: DBConnection, meldekortId: Long, timerArbeid: List<MeldekortDTO.MeldeDag>) {
        return connection.executeBatch(
            """INSERT INTO MELDEKORT_ARBEIDS_PERIODE(MELDEKORT_ID, DATO, TIMER_ARBEIDET) VALUES (?, ?, ?)""".trimIndent(),
            timerArbeid
        ){
            setParams {it ->
                setLong(1, meldekortId)
                setLocalDate(2, it.dag)
                setBigDecimal(3, it.timerArbeidet)
            }
        }
    }

    fun hentAlle(personIdentifikatorer: List<String>, fom: LocalDate? = null, tom: LocalDate? = null): List<MeldekortDTO> {
        val iMorgen = LocalDate.now().plusDays(1)
        require(fom == null || fom.isBefore(iMorgen)) {
            "Kan ikke hente meldekort fra og med en fremtidig dato"
        }
        return connection.queryList<MeldekortDTO>(
            """SELECT * FROM MELDEKORT WHERE PERSONIDENT IN ?
                AND ( ? IS NULL OR MOTTATT_TIDSPUNKT >= ?::date) AND (? IS NULL OR MOTTATT_TIDSPUNKT<= ?::date)
                ORDER BY MOTTATT_TIDSPUNKT DESC
            """.trimIndent()
        ){
            setParams {
                setArray(1, personIdentifikatorer)
                setLocalDate(2, fom)
                setLocalDate(3, tom)
            }
            setRowMapper { row ->
                rowToMeldekortDTO(row)
            }
        }
    }

    private fun rowToMeldekortDTO(row: Row): MeldekortDTO{
        MeldekortDTO(
            personIdent = row.getString("PERSONIDENT"),
            saksnummer = no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer(row.getString("SAKSNUMMER")),
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
                WHERE MELDEPERIODE_ID = ?
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