package api.postgres

import api.PostgresConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.core.instrument.MeterRegistry
import org.flywaydb.core.Flyway
import java.sql.ResultSet
import javax.sql.DataSource

internal object Hikari {
    fun createAndMigrate(
        config: PostgresConfig,
        locations: Array<String> = arrayOf("classpath:db/migration", "classpath:db/gcp"),
        meterRegistry: MeterRegistry? = null
    ): DataSource {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.url
            username = config.username
            password = config.password
            maximumPoolSize = 3
            minimumIdle = 1
            initializationFailTimeout = 5000
            sslmode = ""
            idleTimeout = 10001
            connectionTimeout = 1000
            driverClassName = config.driver
            metricRegistry = meterRegistry
        }

        return createAndMigrate(hikariConfig, locations)
    }

    fun createAndMigrate(
        config: HikariConfig,
        locations: Array<String>
    ): DataSource {
        val dataSource = HikariDataSource(config)

        Flyway
            .configure()
            .dataSource(dataSource)
            .locations(*locations)
            .validateMigrationNaming(true)
            .load()
            .migrate()

        return dataSource
    }
}

fun <T : Any> ResultSet.map(block: (ResultSet) -> T): List<T> =
    sequence {
        while (next()) yield(block(this@map))
    }.toList()

