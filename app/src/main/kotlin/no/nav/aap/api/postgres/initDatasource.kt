package no.nav.aap.api.postgres

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.api.DbConfig
import java.util.Properties
import javax.sql.DataSource

private val postgresConfig = Properties().apply {
    put("tcpKeepAlive", true) // kreves av Hikari

    put("socketTimeout", 60) // sekunder, makstid for overføring av svaret fra db
    put("statement_timeout", 60_000) // millisekunder, makstid for db til å utføre spørring

    put("logUnclosedConnections", true) // vår kode skal lukke alle connections
    put("logServerErrorDetail", false) // ikke lekk person-data fra queries etc til logger ved feil

    put("assumeMinServerVersion", "16.0") // raskere oppstart av driver
}

fun initDatasource(dbConfig: DbConfig, prometheus: PrometheusMeterRegistry): HikariDataSource =
    HikariDataSource(HikariConfig().apply {
        jdbcUrl = dbConfig.url
        username = dbConfig.username
        password = dbConfig.password
        dataSourceProperties = postgresConfig
        maximumPoolSize = 16
        // do not set minimumIdle, it defaults to maximumPoolSize, matching hikaricp performance recommendations
        driverClassName = "org.postgresql.Driver"
        connectionTestQuery = "SELECT 1"
        metricRegistry = prometheus
    })
