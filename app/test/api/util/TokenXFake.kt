package api.util

import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.TokenXFake() {
    install(ContentNegotiation){
        jackson()
    }

    routing {
        post("/token") {
            call.respond(
                TestToken()
            )
        }
        get("/jwks") {
            call.respondText(TOKEN_X_JWKS)
        }
    }
}

data class TestToken(
    val exprires_in:Int=3599,
    val access_token: String="very.secure.token"
)

