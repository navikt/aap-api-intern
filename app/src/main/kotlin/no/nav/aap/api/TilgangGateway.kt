package no.nav.aap.api

import java.net.URI
import java.time.Duration
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.retryablePost
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureOBOTokenProvider
import no.nav.aap.tilgang.PersonTilgangRequest
import no.nav.aap.tilgang.TilgangResponse

object TilgangGateway {
    private val baseUrl = URI.create(requiredConfigForKey("integrasjon.tilgang.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.tilgang.scope"))

    private val client =
        RestClient.withDefaultResponseHandler(
            config = config,
            tokenProvider = AzureOBOTokenProvider(),
        )

    fun harTilgangTilPerson(
        personIdent: String,
        token: OidcToken,
    ): Boolean {
        val personTilgangRequest =
            PersonTilgangRequest(
                personIdent = personIdent,
            )
        val httpRequest =
            PostRequest(
                body = personTilgangRequest,
                currentToken = token,
            )


            val respons = requireNotNull(
                client.retryablePost<_, TilgangResponse>(
                    uri = baseUrl.resolve("/tilgang/person"),
                    request = httpRequest,
                )
            )

            return respons.tilgang
    }
}
