package api.util.auth

import io.ktor.client.*
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig

class AzureAdTokenProvider(
    private val config: AzureConfig = AzureConfig(),
    client: HttpClient = defaultHttpClient,
) {
    private val tokenClient = TokenClient(client)

    suspend fun getUsernamePasswordToken(scope: String, username: String, password: String) =
        tokenClient.getAccessToken(config.tokenEndpoint.toString(), username) {
            """
                client_id=${config.clientId}&
                client_secret=${config.clientSecret}&
                scope=$scope&
                username=$username&
                password=$password&
                grant_type=password
            """.asUrlPart()
        }

    suspend fun getClientCredentialToken(scope: String) =
        tokenClient.getAccessToken(config.tokenEndpoint.toString(), scope) {
            """
                client_id=${config.clientId}&
                client_secret=${config.clientSecret}&
                scope=$scope&
                grant_type=client_credentials
            """.asUrlPart()
        }

    suspend fun getOnBehalfOfToken(scope: String, accessToken: String) =
        tokenClient.getAccessToken(config.tokenEndpoint.toString(), scope) {
            """
                client_id=${config.clientId}&
                client_secret=${config.clientSecret}&
                assertion=$accessToken&
                scope=$scope&
                grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&
                requested_token_use=on_behalf_of
            """.asUrlPart()
        }
}

internal fun String.asUrlPart() =
    this.trimIndent().replace("\n", "")