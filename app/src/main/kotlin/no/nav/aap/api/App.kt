package no.nav.aap.api

import com.papsign.ktor.openapigen.model.info.ContactModel
import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.api.arena.ArenaoppslagRestClient
import no.nav.aap.api.arena.IArenaoppslagRestClient
import no.nav.aap.api.kafka.KafkaProducer
import no.nav.aap.api.kafka.ModiaKafkaProducer
import no.nav.aap.api.kafka.ProducerHolder
import no.nav.aap.api.kelvin.dataInsertion
import no.nav.aap.api.pdl.IPdlClient
import no.nav.aap.api.pdl.PdlClient
import no.nav.aap.api.postgres.initDatasource
import no.nav.aap.api.util.registerCircuitBreakerMetrics
import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.json.DeserializationException
import no.nav.aap.komponenter.server.AZURE
import no.nav.aap.komponenter.server.commonKtorModule
import org.slf4j.LoggerFactory
import java.time.LocalDate
import javax.sql.DataSource
import no.nav.aap.api.actuator.actuator
import no.nav.aap.api.util.StatusPagesConfigHelper

private val logger = LoggerFactory.getLogger("App")

fun main() {
    Thread.currentThread()
        .setUncaughtExceptionHandler { _, e -> logger.error("Uhåndtert feil. Type: ${e.javaClass}", e) }
    embeddedServer(Netty, port = 8080, module = Application::api).start(wait = true)
}

fun PrometheusMeterRegistry.httpCallCounter(
    path: String, audience: String, azpName: String
): Counter = this.counter(
    "http_call",
    listOf(Tag.of("path", path), Tag.of("audience", audience), Tag.of("azp_name", azpName))
)

fun PrometheusMeterRegistry.kildesystemTeller(kildesystem: String, path: String): Counter =
    this.counter(
        "api_intern_kildesystem", listOf(Tag.of("kildesystem", kildesystem), Tag.of("path", path))
    )

fun Application.api(
    prometheus: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
    config: Config = Config(),
    datasource: DataSource = initDatasource(config.dbConfig, prometheus),
    arenaRestClient: IArenaoppslagRestClient = ArenaoppslagRestClient(
        config.arenaoppslag, config.azure
    ),
    pdlClient: IPdlClient = PdlClient(),
    nå: LocalDate = LocalDate.now(),
    modiaProducer: KafkaProducer = ModiaKafkaProducer(config.kafka, config.modia),
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

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }

    routing {
        authenticate(AZURE) {
            apiRouting {
                api(datasource, arenaRestClient, prometheus, pdlClient, nå)
                dataInsertion(datasource, modiaProducer)
            }
        }
        actuator(prometheus)
    }
}
