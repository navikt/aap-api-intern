package no.nav.aap.api

import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.retryablePost
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureOBOTokenProvider
import no.nav.aap.tilgang.PersonTilgangRequest
import no.nav.aap.tilgang.TilgangResponse
import java.time.Duration

class TilgangGateway(val config: TilgangGatewayConfig) {

    private val client =
        RestClient.withDefaultResponseHandler(
            config = config.config,
            tokenProvider = AzureOBOTokenProvider(),
        )

    private val cache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(Duration.ofMinutes(15))
        .recordStats()
        .build<String, Boolean>()

    init {
        CaffeineCacheMetrics.monitor(Metrics.prometheus, cache, "tilgang_person_cache")
    }

    fun harTilgangTilPerson(
        personIdent: String,
        token: OidcToken,
    ): Boolean = cache.get("$personIdent:${token.token()}") {
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
                uri = config.baseUrl.resolve("/tilgang/person"),
                request = httpRequest,
            )
        )

        respons.tilgang
    }
}
