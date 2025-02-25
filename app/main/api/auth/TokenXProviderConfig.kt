package oppslag.auth

import java.net.URL

data class TokenXProviderConfig(
    val clientId: String,
    val privateKey: String,
    val tokenEndpoint: String,
    val jwksUrl: URL,
    val issuer: String,
)
