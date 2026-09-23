package no.nav.aap.api.util

import com.fasterxml.jackson.databind.JsonMappingException
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.response.respond
import no.nav.aap.api.IngenTilgangException
import no.nav.aap.komponenter.httpklient.exception.ApiException
import no.nav.aap.komponenter.httpklient.exception.InternfeilException
import no.nav.aap.komponenter.httpklient.exception.IkkeTillattException
import no.nav.aap.komponenter.httpklient.exception.TimeoutException
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.httpklient.httpclient.error.RequestTimeoutHttpResponseException
import no.nav.aap.komponenter.httpklient.httpclient.error.UhåndtertHttpResponsException
import no.nav.aap.komponenter.json.DeserializationException
import org.slf4j.LoggerFactory

object StatusPagesConfigHelper {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun setup(): StatusPagesConfig.() -> Unit = {
        exception<Throwable> { call, cause ->
            val uri = call.request.local.uri

            when (cause) {
                is ApiException -> {
                    logger.warn(cause.message, cause)
                    call.respondWithError(cause)
                }

                is UhåndtertHttpResponsException -> {
                    if (cause.message?.contains("408") == true) {
                        logger.info("Timeout ved kall til '$uri'")
                        call.respondWithError(TimeoutException("Forespørselen tok for lang tid. Prøv igjen om litt."))
                    } else {
                        logger.error("Uhåndtert feil ved HTTP-kall til '$uri'", cause)
                        call.respondWithError(InternfeilException("En feil oppstod under behandling av forespørselen"))
                    }
                }

                is IngenTilgangException -> {
                    logger.info("Mangler tilgang ved kall til '$uri'. ", cause)
                    call.respondWithError(IkkeTillattException(message = "Mangler tilgang"))
                }

                is JsonMappingException,
                is DeserializationException -> {
                    logger.warn("Feil ved deserialisering av request til '$uri'", cause)
                    call.respondWithError(UgyldigForespørselException("Forespørselen inneholder ugyldige data"))
                }

                is IllegalArgumentException -> {
                    logger.warn("Valideringsfeil ved kall til '$uri'", cause)
                    call.respondWithError(UgyldigForespørselException("Forespørselen inneholder ugyldige verdier"))
                }

                is CallNotPermittedException -> {
                    logger.error("Circuit-breaker åpen ved kall til '$uri'", cause)
                    call.respondWithError(
                        ApiException(
                            status = HttpStatusCode.ServiceUnavailable,
                            message = "Tjenesten er midlertidig utilgjengelig",
                        )
                    )
                }

                is RequestTimeoutHttpResponseException,
                is HttpRequestTimeoutException -> {
                    logger.warn("Timeout mot $uri: ", cause)
                    call.respondWithError(TimeoutException("Forespørselen tok for lang tid. Prøv igjen om litt."))
                }

                is ClientRequestException -> when (cause.response.status) {
                    HttpStatusCode.BadRequest -> {
                        logger.warn("Ugyldig forespørsel ved kall til '$uri': ${cause.message}")
                        call.respondWithError(UgyldigForespørselException("Forespørselen inneholder ugyldige verdier"))
                    }
                    else -> {
                        logger.error("Uhåndtert klientfeil ved kall til '$uri': ${cause.response.status}", cause)
                        call.respondWithError(
                            ApiException(
                                status = cause.response.status,
                                message = "En feil oppstod under behandling av forespørselen",
                            )
                        )
                    }
                }

                else -> {
                    logger.error(
                        "Uhåndtert feil ved kall til '$uri' av type ${cause.javaClass.simpleName}",
                        cause
                    )
                    call.respondWithError(InternfeilException("En feil oppstod under behandling av forespørselen"))
                }
            }
        }
    }

    private suspend fun io.ktor.server.application.ApplicationCall.respondWithError(exception: ApiException) {
        respond(
            exception.status,
            exception.tilApiErrorResponse()
        )
    }
}