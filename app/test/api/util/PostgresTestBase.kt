package api.util

import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import javax.sql.DataSource

object PostgresTestBase {
    val dataSource: DataSource = InitTestDatabase.freshDatabase()

    init {
        Migrering.migrate(dataSource, false)
    }
}