package no.nav.aap.api.util

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Lager tokens med samme claim-form som TokenX utsteder etter et bytte av et
 * ID-porten-token: `pid` for innbyggeren og `client_id` for appen som byttet tokenet.
 */
class TokenXTokenGen(
    private val issuer: String = "tokenx",
    private val audience: String = "localhost:aap:api-intern",
) {
    private val rsaKey: RSAKey get() = JWKSet.parse(AZURE_JWKS).getKeyByKeyId("localhost-signer") as RSAKey

    fun generate(clientId: String?, pid: String = "12345678901"): String {
        val now = Date()
        val claims = JWTClaimsSet.Builder()
            .subject(UUID.randomUUID().toString())
            .issuer(issuer)
            .audience(audience)
            .notBeforeTime(now)
            .issueTime(now)
            .expirationTime(Date(now.time + TimeUnit.HOURS.toMillis(1)))
            .claim("pid", pid)
            .claim("acr", "idporten-loa-high")
            .apply { if (clientId != null) claim("client_id", clientId) }
            .build()

        val header = JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.keyID).type(JOSEObjectType.JWT).build()
        return SignedJWT(header, claims).apply { sign(RSASSASigner(rsaKey.toPrivateKey())) }.serialize()
    }
}
