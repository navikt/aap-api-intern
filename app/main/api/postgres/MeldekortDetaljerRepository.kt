package api.postgres

import api.kelvin.MeldekortDTO
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import org.slf4j.LoggerFactory
import java.time.LocalDate

class MeldekortDetaljerRepository(private val connection: DBConnection) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun lagre(meldekortForSammeSak: List<MeldekortDTO>) {
        if (meldekortForSammeSak.isEmpty()) return

        val etMeldekort = meldekortForSammeSak.first()
        val behandlingId = etMeldekort.behandlingId
        val saksnummer = etMeldekort.saksnummer

        val nyesteBehandlingId = hentNyesteBehandlingIdLagretForSaken(saksnummer)

        nyesteBehandlingId?.let {
            // Vi har meldekort fra før for samme sak, og vil bare beholde de nyeste.

            if (behandlingId < nyesteBehandlingId) {
                log.warn(
                    "Avviser oppdatering av meldekort for sak $saksnummer med behandlingId $behandlingId, " +
                            "har allerede mottatt behandlingId $nyesteBehandlingId"
                )
                return
            }

            slettGamleMeldekort(saksnummer, behandlingId)
        }

        lagreNyeMeldekort(meldekortForSammeSak)
    }

    private fun lagreNyeMeldekort(meldekortForSammeSak: List<MeldekortDTO>) {
        meldekortForSammeSak.forEach {
            val meldekortId = insertMeldekort(connection, it)
            insertTimerArbeidet(connection, meldekortId, it.arbeidPerDag)
        }
    }

    private fun slettGamleMeldekort(saksnummer: String, behandlingId: Long) {
        connection.execute(
            """
                    DELETE FROM MELDEKORT
                    WHERE SAKSNUMMER = ? and BEHANDLING_ID <= ?
                """.trimIndent()
        ) {
            setParams {
                setString(1, saksnummer)
                setLong(2, behandlingId)
            }
        }
    }

    private fun hentNyesteBehandlingIdLagretForSaken(saksnummer: String): Long? = connection.queryFirstOrNull(
        """
                    select max(BEHANDLING_ID) as nyeste FROM MELDEKORT
                    WHERE SAKSNUMMER = ?
                """.trimIndent()
    ) {
        setParams {
            setString(1, saksnummer)
        }

        setRowMapper { row ->
            row.getLongOrNull("nyeste")
        }
    }

    private fun insertMeldekort(connection: DBConnection, meldekort: MeldekortDTO): Long {
        return connection.executeReturnKey(
            """INSERT INTO MELDEKORT(
                |PERSON_IDENT, MOTTATT_TIDSPUNKT, SAKSNUMMER, BEHANDLING_ID, 
                |PERIODE, MELDEPLIKTSTATUSKODE, RETTIGHETSTYPEKODE) 
                |VALUES (?, ?, ?, ?, ?::daterange, ?, ?)""".trimMargin()
        ) {
            setParams {
                setString(1, meldekort.personIdent)
                setLocalDateTime(2, meldekort.mottattTidspunkt)
                setString(3, meldekort.saksnummer)
                setLong(4, meldekort.behandlingId)
                setPeriode(5, meldekort.meldePeriode)
                setString(6, meldekort.meldepliktStatusKode)
                setString(7, meldekort.rettighetsTypeKode)
            }
        }
    }

    private fun insertTimerArbeidet(
        connection: DBConnection,
        meldekortId: Long,
        timerArbeid: List<MeldekortDTO.MeldeDag>
    ) {
        connection.executeBatch(
            """INSERT INTO MELDEKORT_ARBEIDS_PERIODE(MELDEKORT_ID, DATO, TIMER_ARBEIDET) VALUES (?, ?, ?)""".trimIndent(),
            timerArbeid
        ) {
            setParams { it ->
                setLong(1, meldekortId)
                setLocalDate(2, it.dag)
                setBigDecimal(3, it.timerArbeidet)
            }
        }
        println("done")
    }

    fun hentAlle(
        personIdentifikatorer: List<String>,
        fom: LocalDate? = null,
        tom: LocalDate? = null
    ): List<MeldekortDTO> {
        val iMorgen = LocalDate.now().plusDays(1)
        require(fom == null || fom.isBefore(iMorgen)) {
            "Kan ikke hente meldekort fra og med en fremtidig dato"
        }
        return connection.queryList(
            """SELECT * FROM MELDEKORT WHERE PERSON_IDENT = ANY(?)
                AND ( ? IS NULL OR MOTTATT_TIDSPUNKT >= ?::date) 
                AND ( ? IS NULL OR MOTTATT_TIDSPUNKT <= ?::date)
            """.trimIndent()
        ) {
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

    private fun rowToMeldekortDTO(row: Row): MeldekortDTO {
        return MeldekortDTO(
            personIdent = row.getString("PERSON_IDENT"),
            saksnummer = row.getString("SAKSNUMMER"),
            behandlingId = row.getLong("BEHANDLING_ID"),
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
        ) {
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