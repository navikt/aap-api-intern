package api.util

import api.auth.SamtykkeIkkeGittException
import io.ktor.http.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.slf4j.Logger

fun StatusPagesConfig.feilhåndtering(logger: Logger) {
    exception<SamtykkeIkkeGittException> { call, cause ->
        logger.warn("Samtykke ikke gitt", cause)
        call.respondText(text = "Samtykke ikke gitt", status = HttpStatusCode.Forbidden)
    }
    exception<ContentTransformationException> { call, cause ->
        logger.warn("Feil i mottatte data", cause)
        call.respondText(text = "Feil i mottatte data", status = HttpStatusCode.BadRequest)
    }
    exception<IllegalArgumentException> { call, cause ->
        logger.warn("Feil i mottatte data", cause)
        call.respondText(text = "Feil i mottatte data", status = HttpStatusCode.BadRequest)
    }
    exception<Throwable> { call, cause ->
        logger.error("Uhåndtert feil", cause)
        call.respondText(text = "Feil i tjeneste: ${cause.message}", status = HttpStatusCode.InternalServerError)
    }
}
