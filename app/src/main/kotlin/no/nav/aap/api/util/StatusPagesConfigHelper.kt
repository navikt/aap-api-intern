package no.nav.aap.api.util

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
                        logger.info("Timeout ved kall til '$uri': ", cause)
                        call.respond(HttpStatusCode.RequestTimeout)
                    } else {
                        logger.error("Uhåndtert feil ved kall til '$uri'. ", cause)
                        call.respondText(
                            "Ukjent feil i tjeneste: ${cause.message}",
                            status = HttpStatusCode.InternalServerError
                        )
                    }
                }

                is IngenTilgangException -> {
                    logger.info("Mangler tilgang ved kall til '$uri'. ", cause)
                    call.respond(HttpStatusCode.Forbidden)
                }

                is DeserializationException -> {
                    logger.warn("Feil ved deserialisering av request til '$uri'. ", cause)
                    call.respondText(
                        text = "Feil i mottatte data: ${cause.message}",
                        status = HttpStatusCode.BadRequest
                    )
                }

                is IllegalArgumentException -> {
                    logger.warn("Valideringsfeil ved kall til '$uri'. ", cause)
                    call.respondText(
                        "Valideringsfeil. ${cause.message}", status = HttpStatusCode.BadRequest
                    )
                }

                else -> {
                    logger.error("Uhåndtert feil ved kall til '$uri'. ", cause)
                    call.respondText(
                        text = "Feil i tjeneste: ${cause.message}",
                        status = HttpStatusCode.InternalServerError
                    )
                }
            }
        }
    }
}