package api.util

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import oppslag.auth.TokenXProviderConfig
import org.intellij.lang.annotations.Language
import java.util.*

internal class TokenXGen(private val config: TokenXProviderConfig) {

    companion object {
        public val rsaKey: RSAKey get() = JWKSet.parse(TOKEN_X_JWKS).getKeyByKeyId("localhost-signer") as RSAKey
    }


    private fun signed(claims: JWTClaimsSet): SignedJWT {
        val header = JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.keyID).type(JOSEObjectType.JWT).build()
        val signer = RSASSASigner(rsaKey.toPrivateKey())
        return SignedJWT(header, claims).apply { sign(signer) }
    }

    private fun claims(personident: String = "12345678910") = JWTClaimsSet
        .Builder()
        .issuer(config.issuer)
        .audience(config.clientId)
        .expirationTime(Date(Date().time + 60 * 60 * 3600))
        .claim("pid", personident)
        .build()

    fun generate(personident: String): String = signed(claims(personident)).serialize()
}

@Language("JSON")
public const val TOKEN_X_JWKS: String = """{
    "keys": [
        {
            "kty": "RSA",
            "d": "MRf73iiXUEhJFxDTtJ5rEHNQsAG8XFuXkz9vXXbMp1_OTo11bEx3SnHiwmO_mSAAeXWNJniLw07V1-nk551h5in_ueAPwXTOf8qddacvDEBZwcxeqfu_Kjh1R0ji8Xn1a037CpH2IO34Lyw2gmsGFdMZgDwa5Z0KJjPCU6W8tF6CA-2omAdNzrFaWtaPFpBC0NzYaaB111bKIXxngG97Cnu81deEEKmX-vL-O4tpvUUybuquxrlFvVlTeYlrQqv50_IKsKSYkg-iu1cbqIiWrRq9eTmA6EppmZbqHjKSM5JYFbPB_oZ9QeHKnp1_MTom-jKMEpw18qq-PzdX_skZWQ",
            "e": "AQAB",
            "use": "sig",
            "kid": "localhost-signer",
            "alg": "RS256",
            "n": "lFTMP9TSUwLua0G8M7foqmdUS2us1-JOF8H_tClVG3IEQMRvMmHJoGSdldWDHsNwRG3Wevl_8fZoGocw9hPqj93j-vI4-ZkbxwhPyRqlS0FNIPD1Ln5R6AmHu7b-paRIz3lvqpyTRwnGBI9weE4u6WOpOQ8DjJMNPq4WcM42AgDJAvc6UuhcWW_MLIsjkKp_VYKxzthSuiRAxXi8Pz4ZhiTAEZI-UN61DYU9YEFNujg5XtIQsRwQn1Vj7BknGwkdf_iCGJgDlKUOz9hAojOMXTAwetUx6I5nngIM5vaXWJCmKn6SzcTYgHWWVrn8qaSazioaydLaYN9NuQ0MdIvsQw"
        }
    ]
}"""
