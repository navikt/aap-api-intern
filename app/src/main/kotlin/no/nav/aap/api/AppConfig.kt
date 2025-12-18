package no.nav.aap.api

import no.nav.aap.api.kafka.KafkaConfig
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import java.net.URI
import kotlin.time.Duration.Companion.seconds


private fun getEnvVar(envar: String) = System.getenv(envar) ?: error("missing envvar $envar")

data class AppConfig(
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
){
    companion object {
        // Vi endrer ktor sin default-verdi som er "antall CPUer" synlige for JVM-en, som normalt er antall tilgjengelige kjener på container-hosten.
        // Dette kan gi et for høyt antall tråder i forhold. På den andre siden har vi en del venting på IO (db, http-auth).
        // Sett den til en balansert verdi:
        val ktorParallellitet: Int = 16 // defaulter ellers til 4 pga "-XX:ActiveProcessorCount=4" i Dockerfile

        // Matcher terminationGracePeriodSeconds for podden i Kubernetes-manifestet ("nais.yaml")
        private val kubernetesTimeout = 30.seconds

        // Tid før ktor avslutter uansett. Må være litt mindre enn `kubernetesTimeout`.
        val shutdownTimeout = kubernetesTimeout - 2.seconds

        // Tid appen får til å fullføre påbegynte requests, jobber etc. Må være mindre enn `shutdownTimeout`.
        val shutdownGracePeriod = shutdownTimeout - 3.seconds
    }
}

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