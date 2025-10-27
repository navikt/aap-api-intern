package api.util

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.junit.jupiter.api.BeforeEach
import org.slf4j.bridge.SLF4JBridgeHandler
import java.util.logging.Level
import javax.sql.DataSource

abstract class PostgresTestBase {
    val dataSource = TestDataSource()

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

    fun countMeldekort(): Int =
        dataSource.transaction { con ->
            con.queryFirst("SELECT count(*) as nr FROM MELDEKORT"){
                setRowMapper { row -> row.getInt("nr") }
            }
        }


}