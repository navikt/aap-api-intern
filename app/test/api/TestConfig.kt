package api

import api.util.Fakes
import api.util.port
import no.nav.aap.ktor.client.auth.azure.AzureConfig

object TestConfig {
    internal val postgres = DbConfig(
        username = "sa",
        password = "",
        url = "jdbc:h2:mem:test_db;MODE=PostgreSQL",
    )

    val azure = AzureConfig(
        tokenEndpoint = "http://localhost:${Fakes().azure.port()}/jwt",
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
            azure = azure,
            dbConfig = postgres

        )
    }

}