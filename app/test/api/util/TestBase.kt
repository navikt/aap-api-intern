package api.util

import api.TestConfig
import api.api
import api.kelvin.MeldekortPerioderDTO
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.TestApplication
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbmigrering.Migrering
import org.junit.jupiter.api.BeforeEach
import javax.sql.DataSource

abstract class TestBase(
    protected val dataSource: DataSource,
) {
    val azure = AzureTokenGen("test", "test")
    var httpClient: HttpClient

    init {
        Migrering.migrate(dataSource, false)

        val testApplication =
            TestApplication {
                application {
                    Fakes().use { fakes ->
                        api(
                            config = TestConfig.default(fakes),
                            datasource = dataSource,
                            arenaRestClient = ArenaClient(),
                        )
                    }
                }
            }

        httpClient =
            testApplication.createClient {
                install(ContentNegotiation) {
                    jackson {
                        registerModule(JavaTimeModule())
                        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    }
                }
            }
    }

    @BeforeEach
    fun clearTables() {
        this.dataSource.transaction { con ->
            con.execute("TRUNCATE TABLE MELDEKORT_PERIODER_MED_FNR")
            con.execute("TRUNCATE TABLE SAKER")
        }
    }

    fun countSaker(): Int? =
        this.dataSource.transaction { con ->
            con.queryFirstOrNull("SELECT count(*) as nr FROM SAKER") {
                setRowMapper { row -> row.getInt("nr") }
            }
        }

    fun countMeldekortEntries(): Int? =
        this.dataSource.transaction { con ->
            con.queryFirstOrNull("SELECT count(*) as nr FROM MELDEKORT_PERIODER_MED_FNR") {
                setRowMapper { row -> row.getInt("nr") }
            }
        }

    fun countTilkjentPerioder(): Int =
        this.dataSource.transaction { con ->
            con.queryFirst("SELECT count(*) as nr FROM TILKJENT_PERIODE") {
                setRowMapper { row -> row.getInt("nr") }
            }
        }

    fun getAllInnsendinger(): List<MeldekortPerioderDTO> =
        this.dataSource.transaction { con ->
            val fnr =
                con.querySet<String>("SELECT fnr FROM MELDEKORT_PERIODER_MED_FNR") {
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
                    },
                )
            }
        }
}
