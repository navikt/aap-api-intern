package no.nav.aap.api

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import java.net.URI

data class TilgangGatewayConfig(
    val baseUrl: URI = URI.create(requiredConfigForKey("integrasjon.tilgang.url")),
    val config: ClientConfig = ClientConfig(scope = requiredConfigForKey("integrasjon.tilgang.scope"))
)