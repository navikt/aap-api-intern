package api.util

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbmigrering.Migrering
import org.junit.jupiter.api.BeforeEach
import javax.sql.DataSource

abstract class PostgresTestBase(val dataSource: DataSource) {
    init {
        Migrering.migrate(dataSource, false)
    }

    @BeforeEach
    fun clearTables() {
        dataSource.transaction { con ->
            con.execute("TRUNCATE TABLE MELDEKORT_PERIODER_MED_FNR")
            con.execute("TRUNCATE TABLE SAKER")
        }

    }

    fun countSaker(): Int? =
        dataSource.transaction { con ->
            con.queryFirstOrNull("SELECT count(*) as nr FROM SAKER"){
                setRowMapper { row -> row.getInt("nr") }
            }
        }

    fun countMeldekortEntries(): Int? =
        dataSource.transaction { con ->
            con.queryFirstOrNull("SELECT count(*) as nr FROM MELDEKORT_PERIODER_MED_FNR"){
                setRowMapper { row -> row.getInt("nr") }

            }
        }

    fun countTilkjentPerioder(): Int =
        dataSource.transaction { con ->
            con.queryFirst("SELECT count(*) as nr FROM TILKJENT_PERIODE"){
                setRowMapper { row -> row.getInt("nr") }
            }
        }


}