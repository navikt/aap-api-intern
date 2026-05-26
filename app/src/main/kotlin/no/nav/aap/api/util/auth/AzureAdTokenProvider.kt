package no.nav.aap.api.util.auth

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import no.nav.aap.komponenter.config.requiredConfigForKey

internal val defaultHttpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }
    }
    install(HttpRequestRetry) {
        retryOnException(maxRetries = 3) // on exceptions during network send, other than timeouts
        exponentialDelay()
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 5_000
    }
}

class AzureAdTokenProvider(private val client: HttpClient = defaultHttpClient) {

    suspend fun getClientCredentialToken(scope: String): String =
        client.post(requiredConfigForKey("nais.token.endpoint")) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "identity_provider" to "entra_id",
                    "target" to scope,
                )
            )
        }
            .body<JsonNode>()
            .get("access_token")
            .asText()
}
