package api.util

import no.nav.aap.ktor.client.auth.azure.AzureConfig
import java.net.URI
import java.net.URL

private fun getEnvVar(envar: String) = System.getenv(envar) ?: error("missing envvar $envar")

data class Config(
    val oauth: OauthConfig = OauthConfig(),
    val arenaoppslag: ArenaoppslagConfig = ArenaoppslagConfig(),
    val azure: AzureConfig = AzureConfig(
        tokenEndpoint = getEnvVar("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
        clientId = getEnvVar("AZURE_APP_CLIENT_ID"),
        clientSecret = getEnvVar("AZURE_APP_CLIENT_SECRET")
    ),
    val kafka: KafkaConfig= KafkaConfig(),
    val sporingslogg: SporingsloggConfig= SporingsloggConfig()
)

data class KafkaConfig(
    val brokers: String = getEnvVar("KAFKA_BROKERS"),
    val truststorePath: String = getEnvVar("KAFKA_TRUSTSTORE_PATH"),
    val keystorePath: String = getEnvVar("KAFKA_KEYSTORE_PATH"),
    val credstorePsw: String= getEnvVar("KAFKA_CREDSTORE_PASSWORD"),
)

data class OauthConfig(
    val maskinporten: MaskinportenConfig= MaskinportenConfig(),
    val samtykke: SamtykkeConfig= SamtykkeConfig()
)

data class SporingsloggConfig(
    val enabled: Boolean = getEnvVar("SPORINGSLOGG_ENABLED").toBoolean(),
    val topic: String= getEnvVar("SPORINGSLOGG_TOPIC")
)

data class MaskinportenConfig(
    val jwksUri: URL= URI(getEnvVar("MASKINPORTEN_JWKS_URI")).toURL(),
    val issuer: IssuerConfig=IssuerConfig(),
    val scope: ScopeConfig= ScopeConfig()
) {
    data class IssuerConfig(
        val name: String= getEnvVar("MASKINPORTEN_ISSUER"),
        val discoveryUrl: String= getEnvVar("MASKINPORTEN_WELL_KNOWN_URL"),
        val audience: String = getEnvVar("AAP_AUDIENCE"),
        val optionalClaims: String = "sub,nbf",
    )

    data class ScopeConfig(
        val afpprivat: String= "nav:aap:afpprivat.read",
        val afpoffentlig: String = "nav:aap:afpoffentlig.read"
    )
}

data class ArenaoppslagConfig(
    val proxyBaseUrl:String = getEnvVar("ARENAOPPSLAG_PROXY_BASE_URL"),
    val scope: String= getEnvVar("ARENAOPPSLAG_SCOPE")
)

data class SamtykkeConfig(
    val wellknownUrl: String= getEnvVar("ALTINN_WELLKNOWN"),
    val audience: String= getEnvVar("ALTINN_AUDIENCE")
)
