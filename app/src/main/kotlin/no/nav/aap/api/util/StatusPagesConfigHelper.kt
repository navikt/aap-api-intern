package no.nav.aap.api.util

import com.fasterxml.jackson.databind.JsonMappingException
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import no.nav.aap.api.IngenTilgangException
import no.nav.aap.komponenter.httpklient.httpclient.error.UhåndtertHttpResponsException
import no.nav.aap.komponenter.json.DeserializationException
import org.slf4j.LoggerFactory

object StatusPagesConfigHelper {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun setup(): StatusPagesConfig.() -> Unit = {
        exception<Throwable> { call, cause ->
            val uri = call.request.local.uri

            when (cause) {
                is UhåndtertHttpResponsException -> {
                    if (cause.message?.contains("408") == true) {
                        logger.info("Timeout ved kall til '$uri'")
                        call.respond(HttpStatusCode.RequestTimeout)
                    } else {
                        logger.error("Uhåndtert feil ved HTTP-kall til '$uri'", cause)
                        call.respondText(
                            text = "En feil oppstod under behandling av forespørselen",
                            status = HttpStatusCode.InternalServerError
                        )
                    }
                }

                is IngenTilgangException -> {
                    logger.info("Mangler tilgang ved kall til '$uri'. ", cause)
                    call.respond(HttpStatusCode.Forbidden)
                }

                is JsonMappingException,
                is DeserializationException -> {
                    logger.warn("Feil ved deserialisering av request til '$uri'", cause)
                    call.respondText(
                        text = "Forespørselen inneholder ugyldige data",
                        status = HttpStatusCode.BadRequest
                    )
                }

                is IllegalArgumentException -> {
                    logger.warn("Valideringsfeil ved kall til '$uri'", cause)
                    call.respondText(
                        text = "Forespørselen inneholder ugyldige verdier",
                        status = HttpStatusCode.BadRequest
                    )
                }

                is CallNotPermittedException -> {
                    logger.error("Circuit-breaker åpen ved kall til '$uri'", cause)
                    call.respondText(
                        text = "Tjenesten er midlertidig utilgjengelig",
                        status = HttpStatusCode.ServiceUnavailable
                    )
                }

                is HttpRequestTimeoutException -> {
                    logger.warn("Timeout mot $uri: ", cause)
                    call.respondText(
                        text = "Forespørselen tok for lang tid. Prøv igjen om litt.",
                        status = HttpStatusCode.RequestTimeout
                    )
                }

                is ClientRequestException -> when (cause.response.status) {
                    HttpStatusCode.BadRequest -> {
                        logger.warn("Ugyldig forespørsel ved kall til '$uri': ${cause.message}")
                        call.respondText(
                            text = "Forespørselen inneholder ugyldige verdier",
                            status = HttpStatusCode.BadRequest
                        )
                    }
                    else -> {
                        logger.error("Uhåndtert klientfeil ved kall til '$uri': ${cause.response.status}", cause)
                        call.respondText(
                            text = "En feil oppstod under behandling av forespørselen",
                            status = HttpStatusCode.InternalServerError
                        )
                    }
                }

                else -> {
                    logger.error(
                        "Uhåndtert feil ved kall til '$uri' av type ${cause.javaClass.simpleName}",
                        cause
                    )
                    call.respondText(
                        text = "En feil oppstod under behandling av forespørselen",
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }
        }
    }
}