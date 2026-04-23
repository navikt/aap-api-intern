package no.nav.aap.api

import java.net.URI
import no.nav.aap.api.kafka.KafkaConfig
import no.nav.aap.api.util.Fakes
import no.nav.aap.api.util.port
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig

object TestConfig {
    internal val postgres = DbConfig(
        username = "sa",
        password = "",
        url = "jdbc:h2:mem:test_db;MODE=PostgreSQL",
    )
    val azure = AzureConfig(
        tokenEndpoint = "http://localhost:${Fakes().azure.port()}/jwt".let(URI::create),
        clientId = "test",
        clientSecret = "test",
        jwksUri = "test",
        issuer = "test",
    )

    fun default(): AppConfig {
        return AppConfig(
            arenaoppslag = ArenaoppslagConfig(
                proxyBaseUrl = "",
                scope = "api://dev-fss.teamdokumenthandtering.dokarkiv/.default"
            ),
            kafka = KafkaConfig(
                brokers = "localhost:2222",
                truststorePath = "test",
                keystorePath = "test",
                credstorePsw = "test"
            ),
            azure = azure,
            dbConfig = postgres,
            modia = ModiaConfig(
                topic = "test"
            )
        )
    }

}