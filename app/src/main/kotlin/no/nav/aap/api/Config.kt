package no.nav.aap.api

import api.kafka.KafkaConfig
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import java.net.URI


private fun getEnvVar(envar: String) = System.getenv(envar) ?: error("missing envvar $envar")

data class Config(
    val arenaoppslag: ArenaoppslagConfig = ArenaoppslagConfig(),
    val kelvinConfig: KelvinConfig = KelvinConfig(),
    val azure: AzureConfig = AzureConfig(
        tokenEndpoint = URI.create(getEnvVar("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT")),
        clientId = getEnvVar("AZURE_APP_CLIENT_ID"),
        clientSecret = getEnvVar("AZURE_APP_CLIENT_SECRET"),
        jwksUri = getEnvVar("AZURE_OPENID_CONFIG_JWKS_URI"),
        issuer = getEnvVar("AZURE_OPENID_CONFIG_ISSUER")
    ),
    val dbConfig: DbConfig = DbConfig(),
    val kafka: KafkaConfig = KafkaConfig(
        brokers = getEnvVar("KAFKA_BROKERS"),
        truststorePath = getEnvVar("KAFKA_TRUSTSTORE_PATH"),
        keystorePath = getEnvVar("KAFKA_KEYSTORE_PATH"),
        credstorePsw = getEnvVar("KAFKA_CREDSTORE_PASSWORD"),
    ),
    val modia: ModiaConfig = ModiaConfig()
)

data class ArenaoppslagConfig(
    val proxyBaseUrl: String = getEnvVar("ARENAOPPSLAG_PROXY_BASE_URL"),
    val scope: String = getEnvVar("ARENAOPPSLAG_SCOPE")
)

data class ModiaConfig(
    val topic: String = getEnvVar("MODIA_TOPIC"),
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