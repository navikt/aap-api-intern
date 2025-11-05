package no.nav.aap.api.util.auth

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.LoggerFactory

internal class CachingTokenClient(private val client: HttpClient) {
    private val cache = TokenCache()
    private val secureLog = LoggerFactory.getLogger("secureLog")

    /**
     * @throws Exception hvis kall til token-endepunkt feiler
     */
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
                // Vi har fått et token og parset det, så det kan trygt caches
                cache.add(cacheKey, it)
            }

        return token.access_token
    }
}