package api.postgres

import api.InitTestDatabase
import api.kelvin.MeldekortPerioderDTO
import no.nav.aap.komponenter.dbconnect.transaction
import org.junit.jupiter.api.BeforeEach
import javax.sql.DataSource

abstract class PostgresTestBase {
    protected val dataSource: DataSource = Hikari.createAndMigrate(
        InitTestDatabase.hikariConfig,
        arrayOf("classpath:db/migration")
    )

    @BeforeEach
    fun clearTables() {
        dataSource.transaction { con ->
            con.execute("TRUNCATE TABLE MELDEKORT_PERIODER_MED_FNR")
        }
    }

    fun countMeldekortEntries(): Int? =
        dataSource.transaction { con ->
            con.queryFirstOrNull("SELECT count(*) as nr FROM MELDEKORT_PERIODER_MED_FNR"){
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
