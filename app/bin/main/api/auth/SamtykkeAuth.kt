package api.auth

import api.util.Config
import com.auth0.jwk.UrlJwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.slf4j.LoggerFactory
import java.security.interfaces.RSAPublicKey
import java.time.LocalDate

private val logger = LoggerFactory.getLogger("SamtykkeAuth")

data class SamtykkeData(
    val samtykkeperiode: Samtykkeperiode,
    val personIdent: String,
    val consumerId: String,
    val samtykketoken: String,
    val tidspunkt: LocalDate = LocalDate.now(),
)

data class Samtykkeperiode(val fraOgMed:LocalDate, val tilOgMed:LocalDate)

class SamtykkeIkkeGittException: Exception {
    constructor(msg: String): super(msg)
    constructor(msg: String, cause: Throwable): super(msg, cause)
}

fun verifiserOgPakkUtSamtykkeToken(token: String, call:ApplicationCall, config: Config): SamtykkeData {
    val samtykkeJwks = SamtykkeJwks(config.oauth.samtykke.wellknownUrl)
    val jwkProvider = UrlJwkProvider(samtykkeJwks.jwksUri)

    val consumerId = hentConsumerId(call)
    val personIdent = hentPersonIdent(call)

    val jwt = JWT.decode(token)
    val jwk = jwkProvider.get(jwt.keyId)

    val publicKey = jwk.publicKey as? RSAPublicKey ?: throw Exception("Invalid key type")

    val algorithm = when (jwk.algorithm) {
        "RS256" -> Algorithm.RSA256(publicKey, null)
        "RSA-OAEP-256" -> Algorithm.RSA256(publicKey, null)
        else -> throw Exception("Unsupported algorithm")
    }

    val verifier = JWT.require(algorithm) // signature
        .withIssuer(samtykkeJwks.issuer)
        .withAudience(config.oauth.samtykke.audience)
        .withClaim("CoveredBy", consumerId)
        .withClaim("OfferedBy", personIdent)
        .build()

    return try {
        verifier.verify(token)
        SamtykkeData(samtykketoken = token, consumerId = consumerId, personIdent = personIdent, samtykkeperiode = parseDates(jwt))
    } catch (e: Exception) {
        logger.info("Token not verified: $e")
        throw SamtykkeIkkeGittException("Klarte ikke godkjenne samtykketoken", e)
    }

}

private fun hentPersonIdent(call: ApplicationCall): String =
    call.request.headers["NAV-PersonIdent"]?: throw SamtykkeIkkeGittException("NAV-PersonIdent ikke satt")

private fun hentConsumerId(call:ApplicationCall): String {
    val principal = requireNotNull(call.principal<JWTPrincipal>())
    val consumer = requireNotNull(principal.payload.getClaim("consumer"))
    return consumer.asMap()["ID"].toString().split(":").last()
}

private fun parseDates(jwt:DecodedJWT):Samtykkeperiode{
    val services = jwt.getClaim("Services").asArray(String::class.java)
    return Samtykkeperiode(
        LocalDate.parse(services[1].split("=").last()),
        LocalDate.parse(services[2].split("=").last())
    )
}
