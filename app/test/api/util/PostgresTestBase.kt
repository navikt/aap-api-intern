package api.util

import no.nav.aap.komponenter.dbtest.InitTestDatabase
import javax.sql.DataSource

import api.kelvin.MeldekortPerioderDTO
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbmigrering.Migrering
import org.junit.jupiter.api.BeforeEach

abstract class PostgresTestBase {
    protected val dataSource: DataSource = InitTestDatabase.dataSource

    init {
        Migrering.migrate(dataSource)
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


    fun getAllInnsendinger(): List<MeldekortPerioderDTO> =
        dataSource.transaction { con ->
            val fnr = con.querySet<String>("SELECT fnr FROM MELDEKORT_PERIODER_MED_FNR"){
                setRowMapper { row ->
                    row.getString("PERSONIDENT")
                }
            }
            fnr.map { fnr ->
                MeldekortPerioderDTO(
                    fnr,
                    con.queryList("""SELECT Perioder FROM MELDEKORT_PERIODER_MED_FNR WHERE PERSONIDENT = ?""") {
                        setParams { setString(1, fnr) }
                        setRowMapper { row ->
                            row.getPeriode("periode")
                        }
                    }
                )
            }
        }
}