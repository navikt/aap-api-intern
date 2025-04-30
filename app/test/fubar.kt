import api.util.PostgresTestBase
import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.dbtest.InitTestDatabase

fun main(){
    val datasource = PostgresTestBase.dataSource
    Migrering.migrate(datasource)
}