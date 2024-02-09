package api.auth

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.net.URL

class SamtykkeJwks (
    private val wellknownUrl: String
) {
    val issuer: String
    val jwksUri: URL

    init {
        val samtykke = runBlocking {
            defaultHttpClient.get(wellknownUrl) {
                accept(ContentType.Application.Json)
            }.body<SamtykkeResponse>()
        }

        issuer = samtykke.issuer
        jwksUri = URI(samtykke.jwksUri).toURL()
    }

    private data class SamtykkeResponse(
        val issuer: String,
        @JsonProperty("jwks_uri")
        val jwksUri: String
    )

    private companion object {
        private val defaultHttpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                jackson {
                    registerModule(JavaTimeModule())
                    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                }
            }
        }
    }
}