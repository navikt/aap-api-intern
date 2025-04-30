package api.util

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import javax.sql.DataSource

object PostgresTestBase {
    val dataSource: DataSource = InitTestDatabase.freshDatabase()

    init {
        Migrering.migrate(dataSource, false)
    }

    fun clearTables() {
        dataSource.transaction { con ->
            con.execute("TRUNCATE TABLE MELDEKORT_PERIODER_MED_FNR")
            con.execute("TRUNCATE TABLE SAKER")
        }

    }

    fun countSaker(): Int? =
        dataSource.transaction { con ->
            con.queryFirstOrNull("SELECT count(*) as nr FROM SAKER") {
                setRowMapper { row -> row.getInt("nr") }
            }
        }

    fun countMeldekortEntries(): Int? =
        dataSource.transaction { con ->
            con.queryFirstOrNull("SELECT count(*) as nr FROM MELDEKORT_PERIODER_MED_FNR") {
                setRowMapper { row -> row.getInt("nr") }

            }
        }

    fun countTilkjentPerioder(): Int =
        dataSource.transaction { con ->
            con.queryFirst("SELECT count(*) as nr FROM TILKJENT_PERIODE") {
                setRowMapper { row -> row.getInt("nr") }
            }
        }
}