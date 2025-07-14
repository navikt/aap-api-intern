package api.util.auth

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.jackson.jackson
import org.slf4j.LoggerFactory

internal val defaultHttpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        }
    }
}

internal class TokenClient(private val client: HttpClient) {
    private val cache = TokenCache()
    private val secureLog = LoggerFactory.getLogger("secureLog")

    suspend fun getAccessToken(tokenEndpoint: String, cacheKey: String, body: () -> String): String {
        val token = cache.get(cacheKey)
            ?: client.post(tokenEndpoint) {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(body())
            }.also {
                if (!it.status.isSuccess()) {
                    secureLog.warn("Feilet token-kall {}: {}", it.status.value, it.bodyAsText())
                }
            }.body<Token>().also {
                cache.add(cacheKey, it)
            }

        return token.access_token
    }
}