package no.nav.aap.api

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.api.arena.ArenaService
import no.nav.aap.api.kafka.KafkaConfig
import no.nav.aap.api.util.FakeArenaGateway
import no.nav.aap.api.util.Fakes
import no.nav.aap.api.util.KafkaFake
import no.nav.aap.api.util.PdlGatewayEmpty
import no.nav.aap.api.util.port
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import java.net.URI

fun main() {
    val fakes = Fakes(azurePort = 8085)
    val dataSource = TestDataSource()
    val kafkaFake = KafkaFake()

    val config = byggAppConfig(fakes)

    println("===========================================")
    println("  TestApp kjører på http://localhost:8084")
    println("  Fake Azure kjører på http://localhost:8085")
    println("  Hent token: POST http://localhost:8085")
    println("===========================================")

    embeddedServer(Netty, port = 8084) {
        api(
            prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            config = config,
            datasource = dataSource,
            arenaService = ArenaService(FakeArenaGateway(), FakeArenaGateway()),
            pdlGateway = PdlGatewayEmpty(),
            modiaProducer = kafkaFake,
        )
        loggStoppOgRyddOpp(fakes, kafkaFake, dataSource)
    }.start(wait = true)
}

private fun byggAppConfig(fakes: Fakes): AppConfig {
    val azurePort = fakes.azure.port()
    return AppConfig(
        arenaoppslag = ArenaoppslagConfig(
            proxyBaseUrl = "http://localhost:${fakes.arena.port()}",
            scope = "test"
        ),
        kelvinConfig = KelvinConfig(
            proxyBaseUrl = "http://localhost:$azurePort",
            scope = "test"
        ),
        azure = AzureConfig(
            tokenEndpoint = URI.create("http://localhost:$azurePort"),
            clientId = "test",
            clientSecret = "test",
            jwksUri = "http://localhost:$azurePort/jwks",
            issuer = "test"
        ),
        dbConfig = DbConfig(
            url = "jdbc:h2:mem:unused",
            username = "unused",
            password = "unused"
        ),
        kafka = KafkaConfig(
            brokers = "localhost:9092",
            truststorePath = "",
            keystorePath = "",
            credstorePsw = ""
        ),
        modia = ModiaConfig(topic = "test-modia-topic")
    )
}

private fun Application.loggStoppOgRyddOpp(
    fakes: Fakes,
    kafkaFake: KafkaFake,
    dataSource: TestDataSource
) {
    monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("TestApp har stoppet")
        fakes.close()
        kafkaFake.close()
        dataSource.close()
        application.monitor.unsubscribe(ApplicationStopped) {}
    }
}
