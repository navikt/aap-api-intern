package api

import no.nav.aap.ktor.client.auth.azure.AzureConfig

private fun getEnvVar(envar: String) = System.getenv(envar) ?: error("missing envvar $envar")

data class Config(
    val arenaoppslag: ArenaoppslagConfig = ArenaoppslagConfig(),
    val kelvinConfig: KelvinConfig = KelvinConfig(),
    val azure: AzureConfig = AzureConfig(
        tokenEndpoint = getEnvVar("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
        clientId = getEnvVar("AZURE_APP_CLIENT_ID"),
        clientSecret = getEnvVar("AZURE_APP_CLIENT_SECRET"),
        jwksUri = getEnvVar("AZURE_OPENID_CONFIG_JWKS_URI"),
        issuer = getEnvVar("AZURE_OPENID_CONFIG_ISSUER")
    ),
    val postgres: PostgresConfig = PostgresConfig()
)

data class ArenaoppslagConfig(
    val proxyBaseUrl:String = getEnvVar("ARENAOPPSLAG_PROXY_BASE_URL"),
    val scope: String= getEnvVar("ARENAOPPSLAG_SCOPE")
)

data class KelvinConfig(
    val proxyBaseUrl:String = getEnvVar("KELVIN_PROXY_BASE_URL"),
    val scope: String= getEnvVar("KELVIN_SCOPE")
)

data class PostgresConfig(
    val host: String = getEnvVar("NAIS_DATABASE_API_API_HOST"),
    val port: String = getEnvVar("NAIS_DATABASE_API_API_PORT"),
    val username: String = getEnvVar("NAIS_DATABASE_API_API_USERNAME"),
    val password: String = getEnvVar("NAIS_DATABASE_API_API_PASSWORD"),
    val database: String = getEnvVar("NAIS_DATABASE_API_API_DATABASE"),
    val url: String = "jdbc:postgresql://${host}:${port}/${database}",
    val driver: String = "org.postgresql.Driver",
    val cluster: String = getEnvVar("NAIS_CLUSTER_NAME")
)
