package no.nav.aap.api

import com.papsign.ktor.openapigen.model.info.ContactModel
import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.time.LocalDate
import javax.sql.DataSource
import no.nav.aap.api.actuator.actuator
import no.nav.aap.api.arena.ArenaoppslagRestClient
import no.nav.aap.api.arena.IArenaoppslagRestClient
import no.nav.aap.api.kafka.KafkaProducer
import no.nav.aap.api.kafka.ModiaKafkaProducer
import no.nav.aap.api.kafka.ProducerHolder
import no.nav.aap.api.kelvin.dataInsertion
import no.nav.aap.api.pdl.IPdlClient
import no.nav.aap.api.pdl.PdlClient
import no.nav.aap.api.postgres.initDatasource
import no.nav.aap.api.util.StatusPagesConfigHelper
import no.nav.aap.api.util.registerCircuitBreakerMetrics
import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.server.AZURE
import no.nav.aap.komponenter.server.commonKtorModule
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("App")

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e -> logger.error("Uhåndtert feil. Type: ${e.javaClass}", e) }

    embeddedServer(Netty, configure = {
        // Vi følger ktor sin metodikk for å regne ut tuning parametre som funksjon av parallellitet
        // https://github.com/ktorio/ktor/blob/3.3.2/ktor-server/ktor-server-core/common/src/io/ktor/server/engine/ApplicationEngine.kt#L30
        connectionGroupSize = AppConfig.ktorParallellitet / 2 + 1
        workerGroupSize = AppConfig.ktorParallellitet / 2 + 1
        callGroupSize = AppConfig.ktorParallellitet

        shutdownGracePeriod = AppConfig.shutdownGracePeriod.inWholeMilliseconds
        shutdownTimeout = AppConfig.shutdownTimeout.inWholeMilliseconds

        connector {
            port = 8080
        }
    }, module = Application::api).start(wait = true)
}

fun Application.api(
    prometheus: PrometheusMeterRegistry = Metrics.prometheus,
    config: AppConfig = AppConfig(),
    datasource: DataSource = initDatasource(config.dbConfig, prometheus),
    arenaRestClient: IArenaoppslagRestClient = ArenaoppslagRestClient(
        config.arenaoppslag, config.azure
    ),
    pdlClient: IPdlClient = PdlClient(),
    nå: LocalDate = LocalDate.now(),
    modiaProducer: KafkaProducer = ModiaKafkaProducer(
        config.kafka, config.modia,
        AppConfig.shutdownGracePeriod
    ),
) {

    Migrering.migrate(datasource)
    registerCircuitBreakerMetrics(prometheus)

    ProducerHolder.setProducer(modiaProducer)

    install(StatusPages, StatusPagesConfigHelper.setup())

    commonKtorModule(
        prometheus = prometheus, azureConfig = AzureConfig(), infoModel = InfoModel(
            title = "aap-api-intern",
            description = "aap-intern-api tilbyr et internt API for henting av aap-data\nBruker Azure til autentisering",
            contact = ContactModel(
                name = "Team AAP",
                url = "https://github.com/navikt/aap-api-intern",
            )
        )
    )

    routing {
        authenticate(AZURE) {
            apiRouting {
                api(datasource, arenaRestClient, pdlClient, nå)
                dataInsertion(datasource, modiaProducer)
            }
        }
        actuator(prometheus)
    }

    monitor.subscribe(ApplicationStarted) { environment ->
        environment.log.info("ktor har startet opp.")
    }
    monitor.subscribe(ApplicationStopPreparing) { environment ->
        environment.log.info("ktor forbereder seg på å stoppe.")
    }
    monitor.subscribe(ApplicationStopping) { environment ->
        environment.log.info("ktor stopper nå å ta imot nye requester, og lar mottatte requester kjøre frem til timeout.")
    }
    monitor.subscribe(ApplicationStopped) { environment ->
        environment.log.info("ktor har fullført nedstoppingen sin. Eventuelle requester og annet arbeid som ikke ble fullført innen timeout ble avbrutt.")
        try {
            modiaProducer.close()
            (datasource as? HikariDataSource)?.close()
        } catch (_: Exception) {
            // Ignorert
        }
    }
}
