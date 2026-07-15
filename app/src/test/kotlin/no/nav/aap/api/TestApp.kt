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
import no.nav.aap.api.util.PdlGatewayEmpty
import no.nav.aap.komponenter.dbtest.TestDataSource

fun main() {
    Fakes.start()
    val dataSource = TestDataSource()

    println("===========================================")
    println("  TestApp kjører på http://localhost:8084")
    println("  Fake Texas kjører på http://localhost:${Fakes.getTexasPort()}")
    println("  Hent token: POST http://localhost:${Fakes.getTexasPort()}/token")
    println("===========================================")

    embeddedServer(Netty, port = 8084) {
        api(
            prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            config = byggAppConfig(),
            datasourceFactory = { dataSource },
            arenaService = ArenaService(FakeArenaGateway(), FakeArenaGateway()),
            pdlGateway = PdlGatewayEmpty(),
            modiaProducer = Fakes.getKafka(),
            aapHendelseProducer = Fakes.getAapHendelse(),
        )
        loggStoppOgRyddOpp(Fakes, dataSource)
    }.start(wait = true)
}

private fun byggAppConfig(): AppConfig {
    return AppConfig(
        arenaoppslag = ArenaoppslagConfig(
            proxyBaseUrl = "http://localhost:${Fakes.getArenaPort()}",
            scope = "test"
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
        modia = ModiaConfig(topic = "test-modia-topic"),
        aapHendelse = AapHendelseConfig(topic = "test-aap-hendelse-topic")
    )
}

private fun Application.loggStoppOgRyddOpp(
    fakes: Fakes,
    dataSource: TestDataSource
) {
    monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("TestApp har stoppet")
        fakes.close()
        dataSource.close()
        application.monitor.unsubscribe(ApplicationStopped) {}
    }
}
