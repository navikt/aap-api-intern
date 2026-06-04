package no.nav.aap.api

import com.papsign.ktor.openapigen.model.info.ContactModel
import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.route
import com.papsign.ktor.openapigen.route.tag
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
import java.time.Clock
import javax.sql.DataSource
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.aap.api.actuator.actuator
import no.nav.aap.api.arena.ArenaService
import no.nav.aap.api.arena.ArenaoppslagGateway
import no.nav.aap.api.kafka.AapHendelseProducer
import no.nav.aap.api.kafka.AapHendelseKafkaProducer
import no.nav.aap.api.kafka.aapHendelseProducerHolder
import no.nav.aap.api.kafka.KafkaProducer
import no.nav.aap.api.kafka.ModiaKafkaProducer
import no.nav.aap.api.kafka.modiaProducerHolder
import no.nav.aap.api.kelvin.dataInsertion
import no.nav.aap.api.motor.ProsesseringsJobber
import no.nav.aap.api.pdl.IPdlGateway
import no.nav.aap.api.pdl.PdlGateway
import no.nav.aap.api.postgres.initDatasource
import no.nav.aap.api.util.StatusPagesConfigHelper
import no.nav.aap.api.util.registerCircuitBreakerMetrics
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.server.auth.IdentityProvider
import no.nav.aap.komponenter.server.commonKtorModule
import no.nav.aap.motor.Motor
import no.nav.aap.motor.api.motorApi
import no.nav.aap.motor.mdc.NoExtraLogInfoProvider
import no.nav.aap.motor.retry.RetryService
import org.slf4j.LoggerFactory
import no.nav.aap.api.kelvin.DokumentinnhentingGateway

private val logger = LoggerFactory.getLogger("App")


fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e ->
        logger.error(
            "Uhåndtert feil. Type: ${e.javaClass}",
            e
        )
    }

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
    arenaService: ArenaService = opprettArenaService(config),
    pdlGateway: IPdlGateway = PdlGateway(),
    clock: Clock = Clock.systemDefaultZone(),
    aapHendelseProducer: AapHendelseProducer = AapHendelseKafkaProducer(
        config.kafka,
        config.aapHendelse,
        AppConfig.shutdownGracePeriod
    ),
    modiaProducer: KafkaProducer = ModiaKafkaProducer(
        config.kafka,
        config.modia,
        AppConfig.shutdownGracePeriod
    ),
) {

    Migrering.migrate(datasource)
    registerCircuitBreakerMetrics(prometheus)
    val motor = module(datasource)

    aapHendelseProducerHolder = aapHendelseProducer
    modiaProducerHolder = modiaProducer

    install(StatusPages, StatusPagesConfigHelper.setup())

    val helperTextIfDev = if (!Miljø.erProd()) {
        " Bruk https://azure-token-generator.intern.dev.nav.no/api/m2m?aud=dev-gcp:aap:api-intern for å få test-token."
    } else ""

    commonKtorModule(
        prometheus = prometheus,
        infoModel = InfoModel(
            title = "aap-api-intern",
            description = "aap-intern-api tilbyr et internt API for henting av aap-data.\nBruker Azure til autentisering." + helperTextIfDev,
            contact = ContactModel(
                name = "Team AAP",
                url = "https://github.com/navikt/aap-api-intern",
            )
        ),
        identityProvider = IdentityProvider.ENTRA_ID
    )

    routing {
        authenticate(IdentityProvider.ENTRA_ID.value) {
            apiRouting {
                tag(Tag.Syfo) {
                    route("/syfo/v1") {
                        dialogmeldingApi(DokumentinnhentingGateway())
                    }
                }
                api(datasource, arenaService, pdlGateway, clock)
                dataInsertion(datasource)
                motorApi(datasource)
            }
        }
        actuator(prometheus, motor)
    }

    monitor.subscribe(ApplicationStarted) { environment ->
        environment.log.info("ktor har startet opp.")
    }
    monitor.subscribe(ApplicationStopPreparing) { env ->
        env.log.info("ktor forbereder seg på å stoppe.")
    }
    monitor.subscribe(ApplicationStopping) { env ->
        env.log.info("ktor stopper nå å ta imot nye requester, og lar mottatte requester kjøre frem til timeout.")
        try {
            // ktor sine eventer kjøres synkront, så vi må kjøre dette asynkront for ikke å blokkere nedstengings-sekvensen
            env.launch(Dispatchers.IO) {
                try {
                    aapHendelseProducer.close()
                } catch (e: Exception) {
                    logger.warn("Feil ved lukking av aapHendelseProducer", e)
                }
                try {
                    modiaProducer.close()
                } catch (e: Exception) {
                    logger.warn("Feil ved lukking av modiaProducer", e)
                }
            }
        } catch (_: Exception) {
            // Ignorert
        }
    }
    monitor.subscribe(ApplicationStopped) { env ->
        env.log.info("ktor har fullført nedstoppingen sin. Eventuelle requester og annet arbeid som ikke ble fullført innen timeout ble avbrutt.")
        try {
            (datasource as? HikariDataSource)?.close()
        } catch (_: Exception) {
            // Ignorert
        }
    }
}

fun Application.module(dataSource: DataSource): Motor {
    val motor = Motor(
        dataSource = dataSource,
        antallKammer = AppConfig.ANTALL_WORKERS,
        logInfoProvider = NoExtraLogInfoProvider,
        jobber = ProsesseringsJobber.alle(),
        prometheus = Metrics.prometheus,
    )

    dataSource.transaction { dbConnection ->
        RetryService(dbConnection).enable()
    }

    monitor.subscribe(ApplicationStarted) {
        motor.start()
    }
    monitor.subscribe(ApplicationStopping) { env ->
        // ktor sine eventer kjøres synkront, så vi må kjøre dette asynkront for ikke å blokkere nedstengings-sekvensen
        env.launch(Dispatchers.IO) {
            motor.stop(AppConfig.stansArbeidTimeout)
        }
    }

    return motor
}

private fun opprettArenaService(config: AppConfig): ArenaService {
    val arenaRestGateway = ArenaoppslagGateway(config.arenaoppslag)
    val arenaHistorikkGateway = ArenaoppslagGateway(
        arenaoppslagConfig = config.arenaoppslag,
        // Vi øker timeouts fordi disse db-queries er tunge
        timeoutMillis = 2.minutes.inWholeMilliseconds,
        slowRequestMillis = 1.minutes.inWholeMilliseconds,
        cacheName = "arenaoppslag_historikk_maksimum_cache",
    )

    return ArenaService(arenaRestGateway, arenaHistorikkGateway)
}
