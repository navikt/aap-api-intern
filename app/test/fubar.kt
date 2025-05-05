import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.dbtest.InitTestDatabase

fun main() {
    val datasource = InitTestDatabase.freshDatabase()
    Migrering.migrate(datasource)
}