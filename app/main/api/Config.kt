package api

import no.nav.aap.ktor.client.auth.azure.AzureConfig
import oppslag.auth.TokenXProviderConfig
import java.net.URI

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
    val dbConfig: DbConfig = DbConfig(),
    val tokenx: TokenXProviderConfig = TokenXProviderConfig(
        clientId = getEnvVar("TOKEN_X_CLIENT_ID"),
        privateKey = getEnvVar("TOKEN_X_PRIVATE_JWK"),
        tokenEndpoint = getEnvVar("TOKEN_X_TOKEN_ENDPOINT"),
        jwksUrl = URI.create(getEnvVar("TOKEN_X_JWKS_URI")).toURL(),
        issuer = getEnvVar("TOKEN_X_ISSUER"),
    ),
)

data class ArenaoppslagConfig(
    val proxyBaseUrl: String = getEnvVar("ARENAOPPSLAG_PROXY_BASE_URL"),
    val scope: String = getEnvVar("ARENAOPPSLAG_SCOPE")
)

data class KelvinConfig(
    val proxyBaseUrl: String = getEnvVar("KELVIN_PROXY_BASE_URL"),
    val scope: String = getEnvVar("KELVIN_SCOPE")
)

class DbConfig(
    val url: String = getEnvVar("NAIS_DATABASE_API_INTERN_API_JDBC_URL"),
    val username: String = System.getenv("NAIS_DATABASE_API_INTERN_API_USERNAME"),
    val password: String = System.getenv("NAIS_DATABASE_API_INTERN_API_PASSWORD")
)

