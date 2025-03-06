package api

import api.arena.ArenaoppslagRestClient
import api.kelvin.KelvinClient
import api.kelvin.dataInsertion
import api.postgres.initDatasource
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
import no.nav.aap.komponenter.dbmigrering.Migrering
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.server.AZURE
import no.nav.aap.komponenter.server.commonKtorModule
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode

private val logger = LoggerFactory.getLogger("App")

fun main() {
    Thread.currentThread().setUncaughtExceptionHandler { _, e -> logger.error("Uhåndtert feil", e) }
    embeddedServer(Netty, port = 8080, module = Application::api).start(wait = true)
}

fun PrometheusMeterRegistry.httpCallCounter(
    path: String,
    audience: String,
    azpName: String
): Counter = this.counter(
    "http_call",
    listOf(Tag.of("path", path), Tag.of("audience", audience), Tag.of("azp_name", azpName))
)

fun Application.api(
    prometheus: PrometheusMeterRegistry=PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
    config: Config = Config(),
    datasource: DataSource = initDatasource(config.dbConfig, prometheus)
) {
    val arenaRestClient = ArenaoppslagRestClient(config.arenaoppslag, config.azure)
    val kelvin = KelvinClient(config.kelvinConfig)
    Migrering.migrate(datasource, config.wipeDb)

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Uhåndtert feil ved kall til '{}'", call.request.local.uri, cause)
            call.respondText(text = "Feil i tjeneste: ${cause.message}", status = HttpStatusCode.InternalServerError)
        }
    }

    commonKtorModule(
        prometheus = prometheus,
        azureConfig = AzureConfig(),
        infoModel = InfoModel(
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
                api(datasource, arenaRestClient, kelvin, prometheus)
                dataInsertion(datasource)
            }
        }
        actuator(prometheus)
    }
}
