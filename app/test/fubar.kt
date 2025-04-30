import api.util.PostgresTestBase
import no.nav.aap.komponenter.dbmigrering.Migrering

fun main(){
    val datasource = PostgresTestBase.dataSource
    Migrering.migrate(datasource)
}