package api.util.auth

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig

internal val defaultHttpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }
    }
    install(HttpRequestRetry) {
        retryOnServerErrors(maxRetries = 3)
        exponentialDelay()
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 5_000
    }
}

class AzureAdTokenProvider(
    private val config: AzureConfig = AzureConfig(),
    client: HttpClient = defaultHttpClient,
) {
    private val cachingTokenClient = CachingTokenClient(client)

    suspend fun getClientCredentialToken(scope: String) =
        cachingTokenClient.getAccessToken(config.tokenEndpoint.toString(), scope) {
            """
                client_id=${config.clientId}&
                client_secret=${config.clientSecret}&
                scope=$scope&
                grant_type=client_credentials
            """.asUrlPart()
        }

}

internal fun String.asUrlPart() =
    this.trimIndent().replace("\n", "")