package no.nav.aap.api.postgres

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.api.DbConfig
import javax.sql.DataSource

fun initDatasource(dbConfig: DbConfig, prometheus: PrometheusMeterRegistry): DataSource = HikariDataSource(HikariConfig().apply {
    jdbcUrl = dbConfig.url
    username = dbConfig.username
    password = dbConfig.password
    maximumPoolSize = 10
    minimumIdle = 1
    driverClassName = "org.postgresql.Driver"
    connectionTestQuery = "SELECT 1"
    metricRegistry = prometheus
})
