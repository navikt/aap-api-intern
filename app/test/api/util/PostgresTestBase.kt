package api.util

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbmigrering.Migrering
import org.junit.jupiter.api.BeforeEach
import org.slf4j.bridge.SLF4JBridgeHandler
import java.util.logging.Level
import javax.sql.DataSource

abstract class PostgresTestBase(val dataSource: DataSource) {
    init {
        Migrering.migrate(dataSource, false)

        enablePostgresDebugLogs()
    }

    private fun enablePostgresDebugLogs() {
        // Postgres uses java.util.logging, so we need to bridge it to SLF4J
        if (!SLF4JBridgeHandler.isInstalled()) {
            SLF4JBridgeHandler.install()
        }
        // Override the log level from postgres-jdbc's default
        java.util.logging.Logger.getLogger("org.postgresql").level = Level.FINE
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

    fun countMeldekort(): Int =
        dataSource.transaction { con ->
            con.queryFirst("SELECT count(*) as nr FROM MELDEKORT"){
                setRowMapper { row -> row.getInt("nr") }
            }
        }


}