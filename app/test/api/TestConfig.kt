package api

import api.kafka.KafkaConfig
import api.util.Fakes
import api.util.port
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import java.net.URI

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

    fun default(fakes: Fakes): Config {
        return Config(
            arenaoppslag = ArenaoppslagConfig(
                proxyBaseUrl = "",
                scope = "api://dev-fss.teamdokumenthandtering.dokarkiv/.default"
            ),
            kelvinConfig = KelvinConfig(
                proxyBaseUrl = "",
                scope = "api://behandlingsflyt/.default"
            ),
            kafka = KafkaConfig(
                brokers = "localhost:2222",
                truststorePath = "test",
                keystorePath = "test",
                credstorePsw = "test"
            ),
            azure = azure,
            dbConfig = postgres
        )
    }

}